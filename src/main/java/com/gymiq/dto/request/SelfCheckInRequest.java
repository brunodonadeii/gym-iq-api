package com.gymiq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SelfCheckInRequest {

    @NotBlank(message = "Identificador e obrigatorio")
    @Size(max = 150, message = "Identificador deve ter no maximo 150 caracteres")
    private String identifier;

    @NotBlank(message = "Senha e obrigatoria")
    private String password;

    @Size(max = 255, message = "Observacoes devem ter no maximo 255 caracteres")
    private String notes;
}
