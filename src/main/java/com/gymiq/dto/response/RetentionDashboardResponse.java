package com.gymiq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RetentionDashboardResponse {

    private Long activeStudents;
    private Long openAlerts;
    private Long lowRiskStudents;
    private Long mediumRiskStudents;
    private Long highRiskStudents;
    private Long criticalRiskStudents;
    private Double averageRiskScore;
    private Long studentsWithoutCheckInOver15Days;
    private Long studentsWithOverduePayments;
    private List<RetentionAlertResponse> topRiskStudents;
    private LocalDateTime generatedAt;
}
