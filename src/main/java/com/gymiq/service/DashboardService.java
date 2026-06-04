package com.gymiq.service;

import com.gymiq.dto.response.FinancialDashboardResponse;
import com.gymiq.dto.response.OperationsDashboardResponse;
import com.gymiq.dto.response.RetentionAlertResponse;
import com.gymiq.dto.response.RetentionDashboardResponse;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Payment.PaymentStatus;
import com.gymiq.entity.RetentionAlert.AlertStatus;
import com.gymiq.entity.RetentionAlert.RiskLevel;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PaymentRepository;
import com.gymiq.repository.PresenceRepository;
import com.gymiq.repository.RetentionAlertRepository;
import com.gymiq.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int INACTIVITY_DAYS_THRESHOLD = 15;
    private static final int TOP_RISK_LIMIT = 5;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int RATE_SCALE = 2;

    private final RetentionAlertRepository retentionAlertRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final PresenceRepository presenceRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public RetentionDashboardResponse getRetentionDashboard() {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDateTime inactivityLimit = today.minusDays(INACTIVITY_DAYS_THRESHOLD).atStartOfDay();

        return RetentionDashboardResponse.builder()
                .activeStudents(enrollmentRepository.countDistinctStudentsByStatus(EnrollmentStatus.ACTIVE))
                .openAlerts(retentionAlertRepository.countByStatus(AlertStatus.OPEN))
                .lowRiskStudents(countOpenAlertsByRiskLevel(RiskLevel.LOW))
                .mediumRiskStudents(countOpenAlertsByRiskLevel(RiskLevel.MEDIUM))
                .highRiskStudents(countOpenAlertsByRiskLevel(RiskLevel.HIGH))
                .criticalRiskStudents(countOpenAlertsByRiskLevel(RiskLevel.CRITICAL))
                .averageRiskScore(retentionAlertRepository.averageRiskScoreByStatus(AlertStatus.OPEN).orElse(0.0))
                .studentsWithoutCheckInOver15Days(
                        enrollmentRepository.countActiveStudentsWithoutCheckInSince(inactivityLimit))
                .studentsWithOverduePayments((long) paymentRepository.findActiveStudentIdsWithOverduePayments(
                        EnrollmentStatus.ACTIVE,
                        PaymentStatus.OVERDUE,
                        PaymentStatus.PENDING,
                        today).size())
                .topRiskStudents(findTopRiskStudents())
                .generatedAt(LocalDateTime.now(BUSINESS_ZONE))
                .build();
    }

    @Transactional(readOnly = true)
    public FinancialDashboardResponse getFinancialDashboard(LocalDate startDate, LocalDate endDate) {
        DateRange dateRange = resolveDateRange(startDate, endDate);

        BigDecimal paidAmount = sumAmountByStatus(PaymentStatus.PAID, dateRange.startDate(), dateRange.endDate());
        BigDecimal pendingAmount = sumAmountByStatus(PaymentStatus.PENDING, dateRange.startDate(), dateRange.endDate());
        BigDecimal overdueAmount = sumAmountByStatus(PaymentStatus.OVERDUE, dateRange.startDate(), dateRange.endDate());
        BigDecimal projectedRevenue = paidAmount.add(pendingAmount).add(overdueAmount);

        return FinancialDashboardResponse.builder()
                .paidAmountCurrentMonth(paidAmount)
                .pendingAmountCurrentMonth(pendingAmount)
                .overdueAmountCurrentMonth(overdueAmount)
                .projectedRevenueCurrentMonth(projectedRevenue)
                .paidPaymentsCurrentMonth(countPaymentsByStatus(PaymentStatus.PAID, dateRange.startDate(), dateRange.endDate()))
                .pendingPaymentsCurrentMonth(countPaymentsByStatus(PaymentStatus.PENDING, dateRange.startDate(), dateRange.endDate()))
                .overduePaymentsCurrentMonth(countPaymentsByStatus(PaymentStatus.OVERDUE, dateRange.startDate(), dateRange.endDate()))
                .defaultRate(calculateDefaultRate(overdueAmount, projectedRevenue))
                .generatedAt(LocalDateTime.now(BUSINESS_ZONE))
                .build();
    }

    @Transactional(readOnly = true)
    public OperationsDashboardResponse getOperationsDashboard(LocalDate startDate, LocalDate endDate) {
        DateRange dateRange = resolveDateRange(startDate, endDate);
        LocalDateTime startDateTime = dateRange.startDate().atStartOfDay();
        LocalDateTime endDateTimeExclusive = dateRange.endDate().plusDays(1).atStartOfDay();
        Long activeCustomersAtPeriodStart = enrollmentRepository.countActiveCustomersAtDate(
                dateRange.startDate(),
                startDateTime,
                EnrollmentStatus.ACTIVE,
                EnrollmentStatus.CANCELED);
        Long lostCustomersInPeriod = enrollmentRepository.countCanceledCustomersBetween(
                startDateTime,
                endDateTimeExclusive,
                EnrollmentStatus.CANCELED);

        return OperationsDashboardResponse.builder()
                .checkInsToday(presenceRepository.countCheckInsBetween(startDateTime, endDateTimeExclusive))
                .openCheckIns(presenceRepository.countByCheckOutAtIsNull())
                .activeEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.ACTIVE))
                .suspendedEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.SUSPENDED))
                .canceledEnrollments(enrollmentRepository.countByStatus(EnrollmentStatus.CANCELED))
                .enrollmentsExpiringInNext7Days(enrollmentRepository.countByStatusAndEndDateBetween(
                        EnrollmentStatus.ACTIVE,
                        dateRange.startDate(),
                        dateRange.endDate()))
                .newStudentsCurrentMonth(studentRepository.countCreatedBetween(startDateTime, endDateTimeExclusive))
                .activeCustomersAtPeriodStart(activeCustomersAtPeriodStart)
                .lostCustomersInPeriod(lostCustomersInPeriod)
                .churnRate(calculateChurnRate(lostCustomersInPeriod, activeCustomersAtPeriodStart))
                .generatedAt(LocalDateTime.now(BUSINESS_ZONE))
                .build();
    }

    private DateRange resolveDateRange(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate resolvedStartDate = startDate != null ? startDate : today.withDayOfMonth(1);
        LocalDate resolvedEndDate = endDate != null ? endDate : today.withDayOfMonth(today.lengthOfMonth());

        if (resolvedStartDate.isAfter(resolvedEndDate)) {
            throw new BusinessException("Data inicial nao pode ser posterior a data final");
        }

        return new DateRange(resolvedStartDate, resolvedEndDate);
    }

    private Long countOpenAlertsByRiskLevel(RiskLevel riskLevel) {
        return retentionAlertRepository.countByStatusAndRiskLevel(AlertStatus.OPEN, riskLevel);
    }

    private BigDecimal sumAmountByStatus(PaymentStatus status, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = paymentRepository.sumAmountByStatusAndDueDateBetween(status, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    private Long countPaymentsByStatus(PaymentStatus status, LocalDate startDate, LocalDate endDate) {
        return paymentRepository.countByStatusAndDueDateBetween(status, startDate, endDate);
    }

    private BigDecimal calculateDefaultRate(BigDecimal overdueAmount, BigDecimal projectedRevenue) {
        if (projectedRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return overdueAmount
                .multiply(ONE_HUNDRED)
                .divide(projectedRevenue, RATE_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChurnRate(Long lostCustomers, Long activeCustomersAtStart) {
        if (activeCustomersAtStart == null || activeCustomersAtStart == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(lostCustomers)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(activeCustomersAtStart), RATE_SCALE, RoundingMode.HALF_UP);
    }

    private java.util.List<RetentionAlertResponse> findTopRiskStudents() {
        PageRequest topRiskPage = PageRequest.of(
                0,
                TOP_RISK_LIMIT,
                Sort.by(Sort.Order.desc("riskScore"), Sort.Order.desc("updatedAt")));

        return retentionAlertRepository.findByStatus(AlertStatus.OPEN, topRiskPage)
                .map(RetentionAlertResponse::fromEntity)
                .toList();
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }
}
