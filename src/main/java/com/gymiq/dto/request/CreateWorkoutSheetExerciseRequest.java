package com.gymiq.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateWorkoutSheetExerciseRequest {

    @NotNull(message = "ID do exercicio e obrigatorio")
    private Integer exerciseId;

    @NotNull(message = "Series e obrigatorio")
    @Min(value = 1, message = "Series deve ser maior que zero")
    private Integer sets;

    @NotBlank(message = "Repeticoes e obrigatorio")
    @Size(max = 50, message = "Repeticoes deve ter no maximo 50 caracteres")
    private String repetitions;

    @DecimalMin(value = "0.00", message = "Carga nao pode ser negativa")
    private BigDecimal loadKg;

    @Min(value = 0, message = "Descanso nao pode ser negativo")
    private Integer restSeconds;

    @NotNull(message = "Ordem de execucao e obrigatoria")
    @Min(value = 1, message = "Ordem de execucao deve ser maior que zero")
    private Integer executionOrder;

    @Size(max = 255, message = "Observacoes devem ter no maximo 255 caracteres")
    private String notes;
}
