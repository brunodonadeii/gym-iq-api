package com.gymiq.dto.response;

import com.gymiq.entity.Enrollment.EnrollmentStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class StudentDataDeletionEligibilityResponse {

    private UUID studentId;
    private UUID latestEnrollmentId;
    private EnrollmentStatus latestEnrollmentStatus;
    private boolean hasActiveEnrollment;
    private long pendingPayments;
    private long overduePayments;
    private boolean hasFinancialPendingIssues;
    private boolean canAnonymize;
    private List<String> blockers;
}
