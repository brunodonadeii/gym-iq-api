package com.gymiq.dto.response;

import com.gymiq.entity.Presence;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PresenceResponse {

    private Integer presenceId;
    private Integer studentId;
    private String studentName;
    private String studentEmail;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private String notes;
    private LocalDateTime createdAt;

    public static PresenceResponse fromEntity(Presence presence) {
        return PresenceResponse.builder()
                .presenceId(presence.getPresenceId())
                .studentId(presence.getStudent().getStudentId())
                .studentName(presence.getStudent().getUser().getName())
                .studentEmail(presence.getStudent().getUser().getEmail())
                .checkInAt(presence.getCheckInAt())
                .checkOutAt(presence.getCheckOutAt())
                .notes(presence.getNotes())
                .createdAt(presence.getCreatedAt())
                .build();
    }
}
