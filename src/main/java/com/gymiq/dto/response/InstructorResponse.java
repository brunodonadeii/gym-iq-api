package com.gymiq.dto.response;

import com.gymiq.entity.Instructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InstructorResponse {

    private Integer instructorId;
    private Integer userId;
    private String name;
    private String email;
    private String cref;
    private String phone;
    private String specialty;
    private Boolean active;
    private Boolean lgpdAccepted;
    private LocalDateTime createdAt;

    public static InstructorResponse fromEntity(Instructor instructor) {
        return InstructorResponse.builder()
                .instructorId(instructor.getInstructorId())
                .userId(instructor.getUser().getUserId())
                .name(instructor.getUser().getName())
                .email(instructor.getUser().getEmail())
                .cref(instructor.getCref())
                .phone(instructor.getPhone())
                .specialty(instructor.getSpecialty())
                .active(instructor.getUser().getActive())
                .lgpdAccepted(instructor.getUser().getLgpdAccepted())
                .createdAt(instructor.getCreatedAt())
                .build();
    }
}
