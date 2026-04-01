package com.gymiq.service;

import com.gymiq.dto.request.CadastrarAlunoRequest;
import com.gymiq.dto.response.AlunoResponse;
import com.gymiq.entity.Aluno;
import com.gymiq.entity.Usuario;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.AlunoRepository;
import com.gymiq.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AlunoResponse criar(CadastrarAlunoRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("E-mail já cadastrado: " + request.getEmail());
        }
        if (alunoRepository.existsByCpf(request.getCpf())) {
            throw new BusinessException("CPF já cadastrado: " + request.getCpf());
        }

        Usuario usuario = Usuario.builder()
                .nome(request.getNome())
                .email(request.getEmail())
                .senhaHash(passwordEncoder.encode(request.getSenha()))
                .perfil(Usuario.Perfil.ALUNO)
                .ativo(true)
                .lgpdAceito(false)
                .build();
        usuarioRepository.save(usuario);

        Aluno aluno = Aluno.builder()
                .usuario(usuario)
                .cpf(request.getCpf())
                .dataNascimento(request.getDataNascimento())
                .telefone(request.getTelefone())
                .cep(request.getCep())
                .endereco(request.getEndereco())
                .build();
        alunoRepository.save(aluno);

        log.info("Aluno criado: id={}, nome={}", aluno.getIdAluno(), usuario.getNome());
        return AlunoResponse.fromEntity(aluno);
    }

    @Transactional(readOnly = true)
    public List<AlunoResponse> listarTodos() {
        return alunoRepository.findAll()
                .stream()
                .map(AlunoResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlunoResponse> buscar(String termo) {
        return alunoRepository.buscarPorTermo(termo)
                .stream()
                .map(AlunoResponse::fromEntity)
                .toList();
    }


    @Transactional(readOnly = true)
    public AlunoResponse buscarPorId(Integer id) {
        return AlunoResponse.fromEntity(buscarEntidadePorId(id));
    }


    @Transactional
    public AlunoResponse atualizar(Integer id, CadastrarAlunoRequest request) {
        Aluno aluno = buscarEntidadePorId(id);
        Usuario usuario = aluno.getUsuario();


        usuarioRepository.findByEmail(request.getEmail())
                .filter(u -> !u.getIdUsuario().equals(usuario.getIdUsuario()))
                .ifPresent(u -> { throw new BusinessException("E-mail já usado por outro usuário"); });


        alunoRepository.findByCpf(request.getCpf())
                .filter(a -> !a.getIdAluno().equals(id))
                .ifPresent(a -> { throw new BusinessException("CPF já usado por outro aluno"); });

        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        if (request.getSenha() != null && !request.getSenha().isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        }

        aluno.setCpf(request.getCpf());
        aluno.setDataNascimento(request.getDataNascimento());
        aluno.setTelefone(request.getTelefone());
        aluno.setCep(request.getCep());
        aluno.setEndereco(request.getEndereco());

        alunoRepository.save(aluno);
        log.info("Aluno atualizado: id={}", id);
        return AlunoResponse.fromEntity(aluno);
    }

    @Transactional
    public void inativar(Integer id) {
        Aluno aluno = buscarEntidadePorId(id);
        aluno.getUsuario().setAtivo(false);
        alunoRepository.save(aluno);
        log.info("Aluno inativado: id={}", id);
    }


    public Aluno buscarEntidadePorId(Integer id) {
        return alunoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + id));
    }
}
