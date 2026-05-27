package com.gymiq.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateInstructorRequest {

    @NotBlank(message = "Nome e obrigatorio")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;

    @NotBlank(message = "E-mail e obrigatorio")
    @Email(message = "E-mail invalido")
    private String email;

    @NotBlank(message = "CREF e obrigatorio")
    @Size(max = 20, message = "CREF deve ter no maximo 20 caracteres")
    private String cref;

    @NotBlank(message = "Telefone e obrigatorio")
    @Size(max = 20)
    private String phone;

    @Size(max = 100, message = "Especialidade deve ter no maximo 100 caracteres")
    private String specialty;

    @NotNull(message = "Aceite LGPD e obrigatorio")
    private Boolean lgpdAccepted;
}
