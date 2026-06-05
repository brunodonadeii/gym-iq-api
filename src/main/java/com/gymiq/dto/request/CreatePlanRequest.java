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

    @NotBlank(message = "Nome do plano e obrigatorio")
    @Size(min = 2, max = 100)
    private String name;

    @Size(max = 100, message = "Descricao deve ter no maximo 100 caracteres")
    private String description;

    @NotNull(message = "Valor mensal e obrigatorio")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    @DecimalMax(value = "500.00", message = "Valor mensal deve ser no maximo 500.00")
    private BigDecimal monthlyPrice;

    @NotNull(message = "Duracao em meses e obrigatoria")
    @Min(value = 1, message = "Duracao minima e 1 mes")
    @Max(value = 24, message = "Duracao maxima e 24 meses")
    private Integer durationMonths;
}
