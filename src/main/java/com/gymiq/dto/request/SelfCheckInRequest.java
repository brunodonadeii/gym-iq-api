package com.gymiq.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SelfCheckInRequest {

    @NotBlank(message = "Identificador é obrigatório")
    @Size(max = 150, message = "Identificador deve ter no máximo 150 caracteres")
    private String identifier;

    @NotBlank(message = "Senha é obrigatória")
    private String password;

    @Size(max = 255, message = "Observações devem ter no máximo 255 caracteres")
    private String notes;
}
