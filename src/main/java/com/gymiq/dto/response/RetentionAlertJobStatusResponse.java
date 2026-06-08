package com.gymiq.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RetentionAlertJobStatusResponse {

    private UUID jobId;
    private String status;
    private String message;
    private Integer generatedAlerts;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMessage;
}
