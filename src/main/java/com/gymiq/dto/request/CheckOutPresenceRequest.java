package com.gymiq.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CheckOutPresenceRequest {

    private LocalDateTime checkOutAt;

    @Size(max = 255, message = "Observacoes devem ter no maximo 255 caracteres")
    private String notes;
}
