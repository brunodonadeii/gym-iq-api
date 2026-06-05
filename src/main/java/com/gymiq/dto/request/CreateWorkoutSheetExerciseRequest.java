package com.gymiq.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateWorkoutSheetExerciseRequest {

    @NotNull(message = "ID do exercicio e obrigatorio")
    @Min(value = 1, message = "ID do exercicio deve ser maior que zero")
    private Integer exerciseId;

    @NotNull(message = "Series e obrigatorio")
    @Min(value = 1, message = "Series deve ser maior que zero")
    @Max(value = 100, message = "Series deve ser no maximo 100")
    private Integer sets;

    @NotBlank(message = "Repeticoes e obrigatorio")
    @Size(max = 50, message = "Repeticoes deve ter no maximo 50 caracteres")
    private String repetitions;

    @Min(value = 0, message = "Descanso nao pode ser negativo")
    @Max(value = 1200, message = "Descanso deve ser no maximo 1200 segundos")
    private Integer restSeconds;

    @Size(max = 40, message = "Treino deve ter no maximo 40 caracteres")
    private String trainingSection;

    @NotNull(message = "Ordem de execucao e obrigatoria")
    @Min(value = 1, message = "Ordem de execucao deve ser maior que zero")
    @Max(value = 100, message = "Ordem de execucao deve ser no maximo 100")
    private Integer executionOrder;

    @Size(max = 255, message = "Observacoes devem ter no maximo 255 caracteres")
    private String notes;
}
