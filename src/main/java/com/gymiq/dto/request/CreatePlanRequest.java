package com.gymiq.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreatePlanRequest {

    @NotBlank(message = "Nome do plano é obrigatório")
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 100, message = "Descrição deve ter no máximo 100 caracteres")
    private String description;

    @NotNull(message = "Valor mensal é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    @DecimalMax(value = "500.00", message = "Valor mensal deve ser no máximo 500.00")
    private BigDecimal monthlyPrice;

    @NotNull(message = "Duração em meses é obrigatória")
    @Min(value = 1, message = "Duração mínima é 1 mês")
    @Max(value = 24, message = "Duração máxima é 24 meses")
    private Integer durationMonths;
}
