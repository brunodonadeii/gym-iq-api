package com.gymiq.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateInstructorRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String password;

    @NotBlank(message = "CREF é obrigatório")
    @Size(max = 20, message = "CREF deve ter no máximo 20 caracteres")
    private String cref;

    @NotBlank(message = "Telefone é obrigatório")
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
    private String phone;

    @NotBlank(message = "Especialidade é obrigatória")
    @Size(max = 100, message = "Especialidade deve ter no máximo 100 caracteres")
    private String specialty;

    @NotNull(message = "Aceite LGPD é obrigatório")
    @AssertTrue(message = "É necessário aceitar os termos de LGPD para concluir o cadastro")
    private Boolean lgpdAccepted;
}
