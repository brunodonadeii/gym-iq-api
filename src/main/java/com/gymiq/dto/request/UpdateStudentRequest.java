package com.gymiq.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateStudentRequest {

    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    private String name;

    @Email(message = "E-mail invalido")
    private String email;

    @Size(min = 6, message = "Senha deve ter no minimo 6 caracteres")
    private String password;

    @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", message = "CPF deve estar no formato 000.000.000-00")
    private String cpf;

    @Past(message = "Data de nascimento deve ser no passado")
    private LocalDate birthDate;

    @Size(max = 20)
    private String phone;

    @Pattern(regexp = "\\d{5}-\\d{3}", message = "CEP deve estar no formato 00000-000")
    private String zipCode;

    private String address;
}
