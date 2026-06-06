package com.gymiq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateExerciseRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String name;

    @NotBlank(message = "Grupo muscular é obrigatório")
    @Size(max = 80, message = "Grupo muscular deve ter no máximo 80 caracteres")
    private String muscleGroup;

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String description;
}
