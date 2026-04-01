package com.gymiq.service;

import com.gymiq.dto.request.CadastrarPlanoRequest;
import com.gymiq.dto.response.PlanoResponse;
import com.gymiq.entity.Plano;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.PlanoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class PlanoService {

    private final PlanoRepository planoRepository;


    @Transactional
    public PlanoResponse criar(CadastrarPlanoRequest request) {
        if (planoRepository.existsByNomeIgnoreCase(request.getNome())) {
            throw new BusinessException("Já existe um plano com o nome: " + request.getNome());
        }

        Plano plano = Plano.builder()
                .nome(request.getNome())
                .descricao(request.getDescricao())
                .valorMensal(request.getValorMensal())
                .duracaoDias(request.getDuracaoDias())
                .ativo(true)
                .build();

        planoRepository.save(plano);
        log.info("Plano criado: id={}, nome={}", plano.getIdPlano(), plano.getNome());
        return PlanoResponse.fromEntity(plano);
    }

    @Transactional(readOnly = true)
    public List<PlanoResponse> listarAtivos() {
        return planoRepository.findByAtivoTrue()
                .stream()
                .map(PlanoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlanoResponse> listarTodos() {
        return planoRepository.findAll()
                .stream()
                .map(PlanoResponse::fromEntity)
                .toList();
    }


    @Transactional(readOnly = true)
    public PlanoResponse buscarPorId(Integer id) {
        return PlanoResponse.fromEntity(buscarEntidadePorId(id));
    }


    @Transactional
    public PlanoResponse atualizar(Integer id, CadastrarPlanoRequest request) {
        Plano plano = buscarEntidadePorId(id);

        planoRepository.findAll().stream()
                .filter(p -> p.getNome().equalsIgnoreCase(request.getNome())
                             && !p.getIdPlano().equals(id))
                .findFirst()
                .ifPresent(p -> { throw new BusinessException("Nome já usado por outro plano"); });

        plano.setNome(request.getNome());
        plano.setDescricao(request.getDescricao());
        plano.setValorMensal(request.getValorMensal());
        plano.setDuracaoDias(request.getDuracaoDias());

        planoRepository.save(plano);
        log.info("Plano atualizado: id={}", id);
        return PlanoResponse.fromEntity(plano);
    }


    @Transactional
    public void desativar(Integer id) {
        Plano plano = buscarEntidadePorId(id);
        plano.setAtivo(false);
        planoRepository.save(plano);
        log.info("Plano desativado: id={}", id);
    }

    public Plano buscarEntidadePorId(Integer id) {
        return planoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plano não encontrado: " + id));
    }
}
