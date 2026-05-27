package com.gymiq.service;

import com.gymiq.dto.response.FinancialDashboardResponse;
import com.gymiq.dto.response.RetentionAlertResponse;
import com.gymiq.dto.response.RetentionDashboardResponse;
import com.gymiq.entity.Enrollment.EnrollmentStatus;
import com.gymiq.entity.Payment.PaymentStatus;
import com.gymiq.entity.RetentionAlert.AlertStatus;
import com.gymiq.entity.RetentionAlert.RiskLevel;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PaymentRepository;
import com.gymiq.repository.RetentionAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int INACTIVITY_DAYS_THRESHOLD = 15;
    private static final int TOP_RISK_LIMIT = 5;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final RetentionAlertRepository retentionAlertRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public RetentionDashboardResponse getRetentionDashboard() {
        LocalDate today = LocalDate.now();
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
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    public FinancialDashboardResponse getFinancialDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.withDayOfMonth(1);
        LocalDate endDate = today.withDayOfMonth(today.lengthOfMonth());

        BigDecimal paidAmount = sumAmountByStatus(PaymentStatus.PAID, startDate, endDate);
        BigDecimal pendingAmount = sumAmountByStatus(PaymentStatus.PENDING, startDate, endDate);
        BigDecimal overdueAmount = sumAmountByStatus(PaymentStatus.OVERDUE, startDate, endDate);
        BigDecimal projectedRevenue = paidAmount.add(pendingAmount).add(overdueAmount);

        return FinancialDashboardResponse.builder()
                .paidAmountCurrentMonth(paidAmount)
                .pendingAmountCurrentMonth(pendingAmount)
                .overdueAmountCurrentMonth(overdueAmount)
                .projectedRevenueCurrentMonth(projectedRevenue)
                .paidPaymentsCurrentMonth(countPaymentsByStatus(PaymentStatus.PAID, startDate, endDate))
                .pendingPaymentsCurrentMonth(countPaymentsByStatus(PaymentStatus.PENDING, startDate, endDate))
                .overduePaymentsCurrentMonth(countPaymentsByStatus(PaymentStatus.OVERDUE, startDate, endDate))
                .defaultRate(calculateDefaultRate(overdueAmount, projectedRevenue))
                .generatedAt(LocalDateTime.now())
                .build();
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
                .divide(projectedRevenue, 2, RoundingMode.HALF_UP);
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
}
