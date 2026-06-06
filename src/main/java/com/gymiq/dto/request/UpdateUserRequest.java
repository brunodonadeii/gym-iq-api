package com.gymiq.dto.request;

import com.gymiq.entity.User.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotNull(message = "Perfil é obrigatório")
    private Role role;

    @NotNull(message = "Aceite LGPD é obrigatório")
    @AssertTrue(message = "É necessário aceitar os termos de LGPD para concluir a atualização")
    private Boolean lgpdAccepted;
}
