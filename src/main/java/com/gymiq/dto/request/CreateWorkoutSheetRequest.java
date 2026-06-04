package com.gymiq.dto.request;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateWorkoutSheetRequest {

    @NotNull(message = "ID do aluno e obrigatorio")
    private UUID studentId;

    @NotNull(message = "ID do instrutor e obrigatorio")
    private UUID instructorId;

    @NotBlank(message = "Nome da ficha e obrigatorio")
    @Size(max = 100, message = "Nome da ficha deve ter no maximo 100 caracteres")
    private String name;

    @Size(max = 150, message = "Objetivo deve ter no maximo 150 caracteres")
    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 500, message = "Observacoes devem ter no maximo 500 caracteres")
    private String notes;

    @NotEmpty(message = "A ficha deve possuir pelo menos um exercicio")
    private List<@Valid CreateWorkoutSheetExerciseRequest> exercises;
}
