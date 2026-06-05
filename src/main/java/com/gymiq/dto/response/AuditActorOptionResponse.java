package com.gymiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class AuditActorOptionResponse {

    private UUID actorUserId;
    private String actorEmail;
    private String actorRole;
    private String label;

    public AuditActorOptionResponse(UUID actorUserId, String actorEmail, String actorRole) {
        this.actorUserId = actorUserId;
        this.actorEmail = actorEmail;
        this.actorRole = actorRole;
        this.label = actorEmail + " (" + actorRole + ")";
    }
}
