package com.gymiq.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateWorkoutSheetExerciseRequest {

    @NotNull(message = "ID do exercício é obrigatório")
    @Min(value = 1, message = "ID do exercício deve ser maior que zero")
    private Integer exerciseId;

    @NotNull(message = "Séries é obrigatório")
    @Min(value = 1, message = "Séries deve ser maior que zero")
    @Max(value = 100, message = "Séries deve ser no máximo 100")
    private Integer sets;

    @NotBlank(message = "Repetições é obrigatório")
    @Size(max = 50, message = "Repetições deve ter no máximo 50 caracteres")
    private String repetitions;

    @Min(value = 0, message = "Descanso não pode ser negativo")
    @Max(value = 1200, message = "Descanso deve ser no máximo 1200 segundos")
    private Integer restSeconds;

    @Size(max = 40, message = "Treino deve ter no máximo 40 caracteres")
    private String trainingSection;

    @NotNull(message = "Ordem de execução é obrigatória")
    @Min(value = 1, message = "Ordem de execução deve ser maior que zero")
    @Max(value = 100, message = "Ordem de execução deve ser no máximo 100")
    private Integer executionOrder;

    @Size(max = 255, message = "Observações devem ter no máximo 255 caracteres")
    private String notes;
}
