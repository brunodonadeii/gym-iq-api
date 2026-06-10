package com.gymiq.dto.response;

import com.gymiq.entity.Instructor;
import com.gymiq.security.PersonalDataExposurePolicy;
import com.gymiq.security.PersonalDataProtection;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InstructorResponse {

    private UUID instructorId;
    private UUID userId;
    private String name;
    private String email;
    private String cref;
    private String phone;
    private String specialty;
    private Boolean active;
    private Boolean lgpdAccepted;
    private LocalDateTime lgpdAcceptedAt;
    private String lgpdPolicyVersion;
    private String lgpdConsentSource;
    private LocalDateTime createdAt;

    public static InstructorResponse fromEntity(Instructor instructor) {
        boolean fullDataAllowed = PersonalDataExposurePolicy.canViewFullInstructorData();

        return InstructorResponse.builder()
                .instructorId(instructor.getInstructorId())
                .userId(instructor.getUser().getUserId())
                .name(instructor.getUser().getName())
                .email(fullDataAllowed ? instructor.getUser().getEmail() : PersonalDataProtection.maskEmail(instructor.getUser().getEmail()))
                .cref(instructor.getCref())
                .phone(fullDataAllowed ? instructor.getPhone() : PersonalDataProtection.maskPhone(instructor.getPhone()))
                .specialty(instructor.getSpecialty())
                .active(instructor.getUser().getActive())
                .lgpdAccepted(instructor.getUser().getLgpdAccepted())
                .lgpdAcceptedAt(instructor.getUser().getLgpdAcceptedAt())
                .lgpdPolicyVersion(instructor.getUser().getLgpdPolicyVersion())
                .lgpdConsentSource(instructor.getUser().getLgpdConsentSource() != null
                        ? instructor.getUser().getLgpdConsentSource().name()
                        : null)
                .createdAt(instructor.getCreatedAt())
                .build();
    }
}
