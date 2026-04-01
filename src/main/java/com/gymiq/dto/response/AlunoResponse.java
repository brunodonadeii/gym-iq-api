package com.gymiq.dto.response;

import com.gymiq.entity.Aluno;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AlunoResponse {

    private Integer idAluno;
    private Integer idUsuario;
    private String nome;
    private String email;
    private String cpf;
    private LocalDate dataNascimento;
    private String telefone;
    private String cep;
    private String endereco;
    private Boolean ativo;
    private Boolean lgpdAceito;
    private LocalDateTime criadoEm;

    public static AlunoResponse fromEntity(Aluno aluno) {
        return AlunoResponse.builder()
                .idAluno(aluno.getIdAluno())
                .idUsuario(aluno.getUsuario().getIdUsuario())
                .nome(aluno.getUsuario().getNome())
                .email(aluno.getUsuario().getEmail())
                .cpf(aluno.getCpf())
                .dataNascimento(aluno.getDataNascimento())
                .telefone(aluno.getTelefone())
                .cep(aluno.getCep())
                .endereco(aluno.getEndereco())
                .ativo(aluno.getUsuario().getAtivo())
                .lgpdAceito(aluno.getUsuario().getLgpdAceito())
                .criadoEm(aluno.getCriadoEm())
                .build();
    }
}
