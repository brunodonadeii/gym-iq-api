package com.gymiq.dto.request;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateWorkoutSheetRequest {

    @NotNull(message = "ID do aluno é obrigatório")
    private UUID studentId;

    @NotNull(message = "ID do instrutor é obrigatório")
    private UUID instructorId;

    @NotBlank(message = "Nome da ficha é obrigatório")
    @Size(max = 100, message = "Nome da ficha deve ter no máximo 100 caracteres")
    private String name;

    @Size(max = 150, message = "Objetivo deve ter no máximo 150 caracteres")
    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;

    @Size(max = 500, message = "Observações devem ter no máximo 500 caracteres")
    private String notes;

    @NotEmpty(message = "A ficha deve possuir pelo menos um exercício")
    private List<@Valid CreateWorkoutSheetExerciseRequest> exercises;
}
