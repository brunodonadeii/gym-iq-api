package com.gymiq.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentOptionResponse {

    private UUID studentId;
    private String name;
    private String email;
    private String cpf;
    private String label;
}
