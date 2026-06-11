package com.gymiq.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateWorkoutBlockRequest {

    @NotBlank(message = "Nome do treino é obrigatório")
    @Size(max = 60, message = "Nome do treino deve ter no máximo 60 caracteres")
    private String name;

    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    private String description;

    @NotNull(message = "Ordem do treino é obrigatória")
    @Min(value = 1, message = "Ordem do treino deve ser maior que zero")
    @Max(value = 100, message = "Ordem do treino deve ser no máximo 100")
    private Integer executionOrder;

    private List<@Valid CreateWorkoutSheetExerciseRequest> exercises;
}
