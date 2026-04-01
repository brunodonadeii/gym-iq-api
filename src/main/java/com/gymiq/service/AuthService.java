package com.gymiq.service;

import com.gymiq.dto.request.CadastrarAlunoRequest;
import com.gymiq.dto.request.LoginRequest;
import com.gymiq.dto.response.AlunoResponse;
import com.gymiq.dto.response.AuthResponse;
import com.gymiq.entity.Aluno;
import com.gymiq.entity.Usuario;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.AlunoRepository;
import com.gymiq.repository.UsuarioRepository;
import com.gymiq.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final AlunoRepository alunoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;


    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        String token = jwtUtil.gerarToken(
                usuario.getEmail(),
                usuario.getPerfil().name(),
                usuario.getIdUsuario()
        );

        log.info("Login realizado: {} ({})", usuario.getEmail(), usuario.getPerfil());

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .idUsuario(usuario.getIdUsuario())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .perfil(usuario.getPerfil().name())
                .lgpdAceito(usuario.getLgpdAceito())
                .build();
    }


    @Transactional
    public AlunoResponse registrarAluno(CadastrarAlunoRequest request) {
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

        log.info("Novo aluno registrado: {} (id={})", usuario.getEmail(), aluno.getIdAluno());

        return AlunoResponse.fromEntity(aluno);
    }

    @Transactional
    public void registrarAceiteLgpd(Integer idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado: " + idUsuario));

        if (usuario.getLgpdAceito()) {
            throw new BusinessException("LGPD já aceita para este usuário");
        }

        usuario.setLgpdAceito(true);
        usuario.setLgpdAceitoEm(LocalDateTime.now());
        usuarioRepository.save(usuario);

        log.info("LGPD aceita pelo usuário id={} em {}", idUsuario, usuario.getLgpdAceitoEm());
    }
}
