package com.gymiq.service;

import com.gymiq.dto.request.MatricularAlunoRequest;
import com.gymiq.dto.response.MatriculaResponse;
import com.gymiq.entity.Aluno;
import com.gymiq.entity.Matricula;
import com.gymiq.entity.Matricula.StatusMatricula;
import com.gymiq.entity.Plano;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final AlunoService alunoService;
    private final PlanoService planoService;


    @Transactional
    public MatriculaResponse matricular(MatricularAlunoRequest request) {
        Aluno aluno = alunoService.buscarEntidadePorId(request.getIdAluno());
        Plano plano = planoService.buscarEntidadePorId(request.getIdPlano());

        if (!plano.getAtivo()) {
            throw new BusinessException("O plano selecionado está inativo");
        }
        if (!aluno.getUsuario().getAtivo()) {
            throw new BusinessException("O aluno está inativo e não pode ser matriculado");
        }
        if (matriculaRepository.existsByAlunoIdAlunoAndStatus(aluno.getIdAluno(), StatusMatricula.ATIVO)) {
            throw new BusinessException("Aluno já possui uma matrícula ativa. Cancele antes de criar outra.");
        }

        LocalDate inicio = request.getDataInicio() != null ? request.getDataInicio() : LocalDate.now();
        LocalDate fim = inicio.plusDays(plano.getDuracaoDias());

        Matricula matricula = Matricula.builder()
                .aluno(aluno)
                .plano(plano)
                .dataInicio(inicio)
                .dataFim(fim)
                .status(StatusMatricula.ATIVO)
                .build();

        matriculaRepository.save(matricula);
        log.info("Matrícula criada: id={}, aluno={}, plano={}, fim={}",
                matricula.getIdMatricula(), aluno.getIdAluno(), plano.getNome(), fim);

        return MatriculaResponse.fromEntity(matricula);
    }


    @Transactional(readOnly = true)
    public List<MatriculaResponse> listarPorAluno(Integer idAluno) {
        alunoService.buscarEntidadePorId(idAluno); // valida existência
        return matriculaRepository.findByAlunoIdAluno(idAluno)
                .stream()
                .map(MatriculaResponse::fromEntity)
                .toList();
    }


    @Transactional(readOnly = true)
    public MatriculaResponse buscarAtivaDoAluno(Integer idAluno) {
        return matriculaRepository
                .findByAlunoIdAlunoAndStatus(idAluno, StatusMatricula.ATIVO)
                .map(MatriculaResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma matrícula ativa encontrada para o aluno: " + idAluno));
    }


    @Transactional
    public MatriculaResponse alterarStatus(Integer idMatricula, StatusMatricula novoStatus) {
        Matricula matricula = buscarEntidadePorId(idMatricula);

        validarTransicaoDeStatus(matricula.getStatus(), novoStatus);

        matricula.setStatus(novoStatus);
        matriculaRepository.save(matricula);
        log.info("Status da matrícula id={} alterado para {}", idMatricula, novoStatus);

        return MatriculaResponse.fromEntity(matricula);
    }


    @Transactional
    public MatriculaResponse renovar(Integer idMatricula, Integer idNovoPlano) {
        Matricula antiga = buscarEntidadePorId(idMatricula);

        if (antiga.getStatus() == StatusMatricula.CANCELADO) {
            throw new BusinessException("Não é possível renovar uma matrícula cancelada");
        }

        Plano novoPlano = idNovoPlano != null
                ? planoService.buscarEntidadePorId(idNovoPlano)
                : antiga.getPlano();

        if (!novoPlano.getAtivo()) {
            throw new BusinessException("O plano selecionado para renovação está inativo");
        }

        antiga.setStatus(StatusMatricula.CANCELADO);
        matriculaRepository.save(antiga);

        LocalDate inicio = LocalDate.now();
        LocalDate fim = inicio.plusDays(novoPlano.getDuracaoDias());

        Matricula nova = Matricula.builder()
                .aluno(antiga.getAluno())
                .plano(novoPlano)
                .dataInicio(inicio)
                .dataFim(fim)
                .status(StatusMatricula.ATIVO)
                .build();

        matriculaRepository.save(nova);
        log.info("Matrícula renovada: nova id={}, aluno={}, plano={}, fim={}",
                nova.getIdMatricula(), antiga.getAluno().getIdAluno(), novoPlano.getNome(), fim);

        return MatriculaResponse.fromEntity(nova);
    }


    private Matricula buscarEntidadePorId(Integer id) {
        return matriculaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matrícula não encontrada: " + id));
    }

    private void validarTransicaoDeStatus(StatusMatricula atual, StatusMatricula novo) {
        boolean invalida = switch (atual) {
            case CANCELADO -> true; // matrícula cancelada é final
            case ATIVO     -> novo == StatusMatricula.ATIVO;
            case SUSPENSO  -> novo == StatusMatricula.SUSPENSO;
        };
        if (invalida) {
            throw new BusinessException(
                    "Transição de status inválida: %s → %s".formatted(atual, novo));
        }
    }
}
