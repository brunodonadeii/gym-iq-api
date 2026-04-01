package com.gymiq.dto.response;

import com.gymiq.entity.Matricula;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class MatriculaResponse {

    private Integer idMatricula;


    private Integer idAluno;
    private String nomeAluno;
    private String emailAluno;


    private Integer idPlano;
    private String nomePlano;


    private LocalDate dataInicio;
    private LocalDate dataFim;
    private String status;
    private LocalDateTime criadoEm;

    public static MatriculaResponse fromEntity(Matricula m) {
        return MatriculaResponse.builder()
                .idMatricula(m.getIdMatricula())
                .idAluno(m.getAluno().getIdAluno())
                .nomeAluno(m.getAluno().getUsuario().getNome())
                .emailAluno(m.getAluno().getUsuario().getEmail())
                .idPlano(m.getPlano().getIdPlano())
                .nomePlano(m.getPlano().getNome())
                .dataInicio(m.getDataInicio())
                .dataFim(m.getDataFim())
                .status(m.getStatus().name())
                .criadoEm(m.getCriadoEm())
                .build();
    }
}
