package com.gymiq.dto.response;

import com.gymiq.entity.Enrollment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class EnrollmentResponse {

    private Integer enrollmentId;

    private Integer studentId;
    private String studentName;
    private String studentEmail;

    private Integer planId;
    private String planName;

    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private LocalDateTime createdAt;

    public static EnrollmentResponse fromEntity(Enrollment e) {
        return EnrollmentResponse.builder()
                .enrollmentId(e.getEnrollmentId())
                .studentId(e.getStudent().getStudentId())
                .studentName(e.getStudent().getUser().getName())
                .studentEmail(e.getStudent().getUser().getEmail())
                .planId(e.getPlan().getPlanId())
                .planName(e.getPlan().getName())
                .startDate(e.getStartDate())
                .endDate(e.getEndDate())
                .status(e.getStatus().name())
                .createdAt(e.getCreatedAt())
                .build();
    }
}