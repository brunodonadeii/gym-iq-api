package com.gymiq.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PayPaymentRequest {

    private LocalDateTime paidAt;

    @Size(max = 50, message = "Método de pagamento deve ter no máximo 50 caracteres")
    private String paymentMethod;

    @Size(max = 500, message = "Observações devem ter no máximo 500 caracteres")
    private String notes;
}
