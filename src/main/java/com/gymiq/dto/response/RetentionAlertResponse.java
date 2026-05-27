package com.gymiq.dto.response;

import com.gymiq.entity.RetentionAlert;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RetentionAlertResponse {

    private Integer retentionAlertId;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private Integer riskScore;
    private String riskLevel;
    private Integer inactiveDays;
    private Integer overduePayments;
    private String message;
    private String status;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RetentionAlertResponse fromEntity(RetentionAlert alert) {
        return RetentionAlertResponse.builder()
                .retentionAlertId(alert.getRetentionAlertId())
                .studentId(alert.getStudent().getStudentId())
                .studentName(alert.getStudent().getUser().getName())
                .studentEmail(alert.getStudent().getUser().getEmail())
                .riskScore(alert.getRiskScore())
                .riskLevel(alert.getRiskLevel().name())
                .inactiveDays(alert.getInactiveDays())
                .overduePayments(alert.getOverduePayments())
                .message(alert.getMessage())
                .status(alert.getStatus().name())
                .resolvedAt(alert.getResolvedAt())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }
}
