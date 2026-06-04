package com.gymiq.dto.response;

import com.gymiq.entity.Student;
import com.gymiq.security.PersonalDataExposurePolicy;
import com.gymiq.security.PersonalDataProtection;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StudentResponse {

    private UUID studentId;
    private UUID userId;
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
        boolean fullDataAllowed = PersonalDataExposurePolicy.canViewFullStudentData();

        return StudentResponse.builder()
                .studentId(student.getStudentId())
                .userId(student.getUser().getUserId())
                .name(student.getUser().getName())
                .email(fullDataAllowed ? student.getUser().getEmail() : PersonalDataProtection.maskEmail(student.getUser().getEmail()))
                .cpf(fullDataAllowed ? student.getCpf() : PersonalDataProtection.maskCpf(student.getCpf()))
                .birthDate(fullDataAllowed ? student.getBirthDate() : null)
                .phone(fullDataAllowed ? student.getPhone() : PersonalDataProtection.maskPhone(student.getPhone()))
                .zipCode(fullDataAllowed ? student.getZipCode() : null)
                .address(fullDataAllowed ? student.getAddress() : null)
                .active(student.getUser().getActive())
                .lgpdAccepted(student.getUser().getLgpdAccepted())
                .createdAt(student.getCreatedAt())
                .build();
    }
}
