package com.gymiq.dto.response;

import com.gymiq.entity.Student;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class StudentResponse {

    private Integer studentId;
    private Integer userId;
    private String name;
    private String email;
    private String cpf;
    private LocalDate birthDate;
    private String phone;
    private String zipCode;
    private String address;
    private Boolean active;
    private Boolean lgpdAccepted;
    private LocalDateTime createdAt;

    public static StudentResponse fromEntity(Student student) {
        return StudentResponse.builder()
                .studentId(student.getStudentId())
                .userId(student.getUser().getUserId())
                .name(student.getUser().getName())
                .email(student.getUser().getEmail())
                .cpf(student.getCpf())
                .birthDate(student.getBirthDate())
                .phone(student.getPhone())
                .zipCode(student.getZipCode())
                .address(student.getAddress())
                .active(student.getUser().getActive())
                .lgpdAccepted(student.getUser().getLgpdAccepted())
                .createdAt(student.getCreatedAt())
                .build();
    }
}