package com.gymiq.service;

import com.gymiq.dto.request.CreateStudentRequest;
import com.gymiq.dto.request.LoginRequest;
import com.gymiq.dto.response.AuthResponse;
import com.gymiq.dto.response.StudentResponse;
import com.gymiq.entity.User;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.UserRepository;
import com.gymiq.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final StudentService studentService;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getUserId()
        );

        log.info("Login realizado: {} ({})", user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .lgpdAccepted(user.getLgpdAccepted())
                .build();
    }

    @Transactional
    public StudentResponse registerStudent(CreateStudentRequest request) {
        StudentResponse student = studentService.create(request);
        log.info("Novo aluno registrado via auth: {} (id={})", student.getEmail(), student.getStudentId());
        return student;
    }

    @Transactional
    public void registerLgpdAcceptance(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado: " + userId));

        if (user.getLgpdAccepted()) {
            throw new BusinessException("LGPD já aceita para este usuário");
        }

        user.setLgpdAccepted(true);
        user.setLgpdAcceptedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("LGPD aceita pelo usuário id={} em {}", userId, user.getLgpdAcceptedAt());
    }
}
