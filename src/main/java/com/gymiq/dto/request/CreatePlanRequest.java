package com.gymiq.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePlanRequest {

    @NotBlank(message = "Nome do plano é obrigatório")
    @Size(min = 2, max = 100)
    private String name;

    private String description;

    @NotNull(message = "Valor mensal é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal monthlyPrice;

    @NotNull(message = "Duração em dias é obrigatória")
    @Min(value = 1, message = "Duração mínima é 1 dia")
    private Integer durationDays;
}