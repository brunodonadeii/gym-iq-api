package com.gymiq.dto.request;

import jakarta.validation.constraints.DecimalMin;
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

    private String description;

    @NotNull(message = "Valor mensal e obrigatorio")
    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal monthlyPrice;

    @NotNull(message = "Duracao em meses e obrigatoria")
    @Min(value = 1, message = "Duracao minima e 1 mes")
    private Integer durationMonths;
}
