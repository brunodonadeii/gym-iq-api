package com.gymiq.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreatePaymentRequest {

    @NotNull(message = "ID da matricula e obrigatorio")
    private Integer enrollmentId;

    @DecimalMin(value = "0.01", message = "Valor deve ser maior que zero")
    private BigDecimal amount;

    @NotNull(message = "Data de vencimento e obrigatoria")
    private LocalDate dueDate;

    @Size(max = 50, message = "Metodo de pagamento deve ter no maximo 50 caracteres")
    private String paymentMethod;

    @Size(max = 500, message = "Observacoes devem ter no maximo 500 caracteres")
    private String notes;
}
