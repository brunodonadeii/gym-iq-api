package com.gymiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentOptionResponse {

    private Integer studentId;
    private String name;
    private String email;
    private String cpf;
    private String label;
}
