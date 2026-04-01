package com.gymiq.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MatricularAlunoRequest {

    @NotNull(message = "ID do aluno é obrigatório")
    private Integer idAluno;

    @NotNull(message = "ID do plano é obrigatório")
    private Integer idPlano;

    private LocalDate dataInicio;
}
