package com.gymiq.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EnrollStudentRequest {

    @NotNull(message = "ID do aluno é obrigatório")
    private Integer studentId;

    @NotNull(message = "ID do plano é obrigatório")
    private Integer planId;

    private LocalDate startDate;
}