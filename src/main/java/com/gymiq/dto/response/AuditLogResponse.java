package com.gymiq.dto.response;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AuditLogResponse {

    private Long auditLogId;
    private UUID actorUserId;
    private String actorEmail;
    private String actorLabel;
    private String actorRole;
    private AuditAction action;
    private String actionLabel;
    private ResourceType resourceType;
    private String resourceId;
    private String resourceLabel;
    private String description;
    private String ipAddress;
    private LocalDateTime createdAt;
}
