package com.gymiq.service;

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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int INACTIVITY_DAYS_THRESHOLD = 15;
    private static final int TOP_RISK_LIMIT = 5;

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

    private Long countOpenAlertsByRiskLevel(RiskLevel riskLevel) {
        return retentionAlertRepository.countByStatusAndRiskLevel(AlertStatus.OPEN, riskLevel);
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
