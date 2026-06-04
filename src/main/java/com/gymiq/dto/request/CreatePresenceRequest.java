package com.gymiq.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreatePresenceRequest {

    @NotNull(message = "ID do aluno e obrigatorio")
    private UUID studentId;

    private LocalDateTime checkInAt;

    @Size(max = 255, message = "Observacoes devem ter no maximo 255 caracteres")
    private String notes;
}
