package com.gymiq.dto.response;

import com.gymiq.entity.Student;
import com.gymiq.security.PersonalDataExposurePolicy;
import com.gymiq.security.PersonalDataProtection;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StudentSummaryResponse {

    private UUID studentId;
    private UUID userId;
    private String name;
    private String email;
    private Boolean active;
    private Boolean lgpdAccepted;
    private LocalDateTime createdAt;

    public static StudentSummaryResponse fromEntity(Student student) {
        boolean fullDataAllowed = PersonalDataExposurePolicy.canViewFullStudentData();

        return StudentSummaryResponse.builder()
                .studentId(student.getStudentId())
                .userId(student.getUser().getUserId())
                .name(student.getUser().getName())
                .email(fullDataAllowed ? student.getUser().getEmail() : PersonalDataProtection.maskEmail(student.getUser().getEmail()))
                .active(student.getUser().getActive())
                .lgpdAccepted(student.getUser().getLgpdAccepted())
                .createdAt(student.getCreatedAt())
                .build();
    }
}
