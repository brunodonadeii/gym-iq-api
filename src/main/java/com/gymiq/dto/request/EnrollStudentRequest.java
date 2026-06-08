package com.gymiq.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EnrollStudentRequest {

    @NotNull(message = "ID do aluno é obrigatório")
    private UUID studentId;

    @NotNull(message = "ID do plano é obrigatório")
    private Integer planId;

    private LocalDate startDate;
}
