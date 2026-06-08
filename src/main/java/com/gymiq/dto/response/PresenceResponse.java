package com.gymiq.dto.response;

import java.util.UUID;

import com.gymiq.entity.Presence;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PresenceResponse {

    private UUID presenceId;
    private UUID studentId;
    private String studentName;
    private String studentEmail;
    private LocalDateTime checkInAt;
    private String notes;
    private LocalDateTime createdAt;

    public static PresenceResponse fromEntity(Presence presence) {
        return PresenceResponse.builder()
                .presenceId(presence.getPresenceId())
                .studentId(presence.getStudent().getStudentId())
                .studentName(presence.getStudent().getUser().getName())
                .studentEmail(presence.getStudent().getUser().getEmail())
                .checkInAt(presence.getCheckInAt())
                .notes(presence.getNotes())
                .createdAt(presence.getCreatedAt())
                .build();
    }
}
