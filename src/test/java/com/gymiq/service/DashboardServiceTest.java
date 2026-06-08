package com.gymiq.service;

import com.gymiq.dto.response.FinancialDashboardResponse;
import com.gymiq.dto.response.OperationsDashboardResponse;
import com.gymiq.dto.response.RetentionDashboardResponse;
import com.gymiq.entity.Enrollment;
import com.gymiq.entity.Payment;
import com.gymiq.entity.RetentionAlert;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.EnrollmentRepository;
import com.gymiq.repository.PaymentRepository;
import com.gymiq.repository.PresenceRepository;
import com.gymiq.repository.RetentionAlertRepository;
import com.gymiq.repository.StudentRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private RetentionAlertRepository retentionAlertRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PresenceRepository presenceRepository;

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getFinancialDashboardShouldCalculateAmountsCountsAndDefaultRate() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        when(paymentRepository.sumAmountByStatusAndDueDateBetween(Payment.PaymentStatus.PAID, start, end))
                .thenReturn(BigDecimal.valueOf(100));
        when(paymentRepository.sumAmountByStatusAndDueDateBetween(Payment.PaymentStatus.PENDING, start, end))
                .thenReturn(BigDecimal.valueOf(50));
        when(paymentRepository.sumAmountByStatusAndDueDateBetween(Payment.PaymentStatus.OVERDUE, start, end))
                .thenReturn(BigDecimal.valueOf(50));
        when(paymentRepository.countByStatusAndDueDateBetween(Payment.PaymentStatus.PAID, start, end)).thenReturn(1L);
        when(paymentRepository.countByStatusAndDueDateBetween(Payment.PaymentStatus.PENDING, start, end)).thenReturn(2L);
        when(paymentRepository.countByStatusAndDueDateBetween(Payment.PaymentStatus.OVERDUE, start, end)).thenReturn(3L);

        FinancialDashboardResponse response = dashboardService.getFinancialDashboard(start, end);

        assertThat(response.getProjectedRevenueCurrentMonth()).isEqualByComparingTo("200");
        assertThat(response.getDefaultRate()).isEqualByComparingTo("25.00");
        assertThat(response.getPaidPaymentsCurrentMonth()).isEqualTo(1L);
        assertThat(response.getPendingPaymentsCurrentMonth()).isEqualTo(2L);
        assertThat(response.getOverduePaymentsCurrentMonth()).isEqualTo(3L);
    }

    @Test
    void getFinancialDashboardShouldRejectInvalidDateRange() {
        assertThatThrownBy(() -> dashboardService.getFinancialDashboard(
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 6, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Data inicial");
    }

    @Test
    void getFinancialDashboardShouldReturnZeroWhenRepositorySumsAreNull() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        when(paymentRepository.sumAmountByStatusAndDueDateBetween(any(Payment.PaymentStatus.class), eq(start), eq(end)))
                .thenReturn(null);
        when(paymentRepository.countByStatusAndDueDateBetween(any(Payment.PaymentStatus.class), eq(start), eq(end)))
                .thenReturn(0L);

        FinancialDashboardResponse response = dashboardService.getFinancialDashboard(start, end);

        assertThat(response.getProjectedRevenueCurrentMonth()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getDefaultRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getPaidPaymentsCurrentMonth()).isZero();
    }

    @Test
    void getOperationsDashboardShouldCalculateChurnAndOperationalCounters() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);

        when(presenceRepository.countCheckInsBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(20L);
        when(enrollmentRepository.countByStatus(Enrollment.EnrollmentStatus.ACTIVE)).thenReturn(80L);
        when(enrollmentRepository.countByStatus(Enrollment.EnrollmentStatus.SUSPENDED)).thenReturn(5L);
        when(enrollmentRepository.countByStatus(Enrollment.EnrollmentStatus.CANCELED)).thenReturn(10L);
        when(enrollmentRepository.countByStatusAndEndDateBetween(Enrollment.EnrollmentStatus.ACTIVE, start, end)).thenReturn(7L);
        when(studentRepository.countCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(12L);
        when(enrollmentRepository.countActiveCustomersAtDate(eq(start), any(LocalDateTime.class),
                eq(Enrollment.EnrollmentStatus.ACTIVE), eq(Enrollment.EnrollmentStatus.CANCELED))).thenReturn(100L);
        when(enrollmentRepository.countCanceledCustomersBetween(any(LocalDateTime.class), any(LocalDateTime.class),
                eq(Enrollment.EnrollmentStatus.CANCELED))).thenReturn(8L);

        OperationsDashboardResponse response = dashboardService.getOperationsDashboard(start, end);

        assertThat(response.getCheckInsToday()).isEqualTo(20L);
        assertThat(response.getActiveEnrollments()).isEqualTo(80L);
        assertThat(response.getEnrollmentsExpiringInNext7Days()).isEqualTo(7L);
        assertThat(response.getNewStudentsCurrentMonth()).isEqualTo(12L);
        assertThat(response.getChurnRate()).isEqualByComparingTo("8.00");
    }

    @Test
    void getRetentionDashboardShouldAssembleRiskMetricsAndTopStudents() {
        RetentionAlert alert = TestDataFactory.openRetentionAlert();

        when(enrollmentRepository.countActiveCustomersAtDate(any(LocalDate.class), any(LocalDateTime.class),
                eq(Enrollment.EnrollmentStatus.ACTIVE), eq(Enrollment.EnrollmentStatus.CANCELED))).thenReturn(100L);
        when(enrollmentRepository.countCanceledEnrollmentsBetween(any(LocalDateTime.class), any(LocalDateTime.class),
                eq(Enrollment.EnrollmentStatus.CANCELED))).thenReturn(5L);
        when(enrollmentRepository.countActiveStudentsForCurrentOperation(eq(Enrollment.EnrollmentStatus.ACTIVE), any(LocalDate.class)))
                .thenReturn(90L);
        when(retentionAlertRepository.countOpenAlertsForActiveStudents(
                RetentionAlert.AlertStatus.OPEN, Enrollment.EnrollmentStatus.ACTIVE)).thenReturn(10L);
        when(retentionAlertRepository.countOpenAlertsForActiveStudentsByRiskLevel(
                eq(RetentionAlert.AlertStatus.OPEN), eq(Enrollment.EnrollmentStatus.ACTIVE), any(RetentionAlert.RiskLevel.class)))
                .thenReturn(1L);
        when(retentionAlertRepository.averageRiskScoreForActiveStudents(
                RetentionAlert.AlertStatus.OPEN, Enrollment.EnrollmentStatus.ACTIVE)).thenReturn(Optional.of(55.5));
        when(enrollmentRepository.countActiveStudentsWithoutCheckInSince(any(LocalDateTime.class))).thenReturn(3L);
        when(paymentRepository.findActiveStudentIdsWithOverduePayments(
                eq(Enrollment.EnrollmentStatus.ACTIVE),
                eq(Payment.PaymentStatus.OVERDUE),
                eq(Payment.PaymentStatus.PENDING),
                any(LocalDate.class))).thenReturn(List.of(UUID.fromString("00000000-0000-0000-0000-000000000001")));
        when(retentionAlertRepository.findOpenAlertsForActiveStudents(
                eq(RetentionAlert.AlertStatus.OPEN),
                eq(Enrollment.EnrollmentStatus.ACTIVE),
                any(Pageable.class))).thenReturn(new PageImpl<>(List.of(alert)));

        RetentionDashboardResponse response = dashboardService.getRetentionDashboard();

        assertThat(response.getActiveStudents()).isEqualTo(90L);
        assertThat(response.getOpenAlerts()).isEqualTo(10L);
        assertThat(response.getAverageRiskScore()).isEqualTo(55.5);
        assertThat(response.getStudentsWithOverduePayments()).isEqualTo(1L);
        assertThat(response.getChurnRateCurrentMonth()).isEqualByComparingTo("5.00");
        assertThat(response.getTopRiskStudents()).hasSize(1);
    }
}
