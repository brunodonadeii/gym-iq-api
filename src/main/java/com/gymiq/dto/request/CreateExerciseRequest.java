package com.gymiq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateExerciseRequest {

    @NotBlank(message = "Nome e obrigatorio")
    @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres")
    private String name;

    @NotBlank(message = "Grupo muscular e obrigatorio")
    @Size(max = 80, message = "Grupo muscular deve ter no maximo 80 caracteres")
    private String muscleGroup;

    @Size(max = 500, message = "Descricao deve ter no maximo 500 caracteres")
    private String description;
}
