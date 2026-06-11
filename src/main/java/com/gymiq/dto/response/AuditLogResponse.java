package com.gymiq.dto.response;

import com.gymiq.entity.AuditLog;
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
    private String actorRole;
    private AuditAction action;
    private ResourceType resourceType;
    private String resourceId;
    private String description;
    private String ipAddress;
    private LocalDateTime createdAt;

    public static AuditLogResponse fromEntity(AuditLog log) {
        return AuditLogResponse.builder()
                .auditLogId(log.getAuditLogId())
                .actorUserId(log.getActorUserId())
                .actorEmail(log.getActorEmail())
                .actorRole(log.getActorRole())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
