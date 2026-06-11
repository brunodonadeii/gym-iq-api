package com.gymiq.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    private List<@Valid CreateWorkoutBlockRequest> blocks;

    private List<@Valid CreateWorkoutSheetExerciseRequest> exercises;
}
