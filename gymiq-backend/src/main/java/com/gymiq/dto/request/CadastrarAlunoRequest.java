package com.gymiq.dto.request;

import com.gymiq.entity.Usuario.Perfil;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;


@Data
public class CadastrarAlunoRequest {



    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String nome;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;



    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", message = "CPF deve estar no formato 000.000.000-00")
    private String cpf;

    @NotNull(message = "Data de nascimento é obrigatória")
    @Past(message = "Data de nascimento deve ser no passado")
    private LocalDate dataNascimento;

    @NotBlank(message = "Telefone é obrigatório")
    @Size(max = 20)
    private String telefone;

    @Pattern(regexp = "\\d{5}-\\d{3}", message = "CEP deve estar no formato 00000-000")
    private String cep;

    private String endereco;
}
