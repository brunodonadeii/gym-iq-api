package com.gymiq.service;

import com.gymiq.dto.request.CreateUserRequest;
import com.gymiq.dto.response.UserResponse;
import com.gymiq.entity.User;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createAdministrativeUser(CreateUserRequest request) {
        validateAdministrativeRole(request.getRole());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("E-mail ja cadastrado: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true)
                .lgpdAccepted(request.getLgpdAccepted())
                .lgpdAcceptedAt(resolveLgpdAcceptedAt(request.getLgpdAccepted()))
                .build();

        userRepository.save(user);
        log.info("Usuario administrativo criado: id={}, role={}", user.getUserId(), user.getRole());

        return UserResponse.fromEntity(user);
    }

    private void validateAdministrativeRole(User.Role role) {
        if (role == User.Role.STUDENT || role == User.Role.INSTRUCTOR) {
            throw new BusinessException("Use as rotas especificas para criar alunos ou instrutores");
        }
    }

    private LocalDateTime resolveLgpdAcceptedAt(Boolean lgpdAccepted) {
        return Boolean.TRUE.equals(lgpdAccepted) ? LocalDateTime.now() : null;
    }
}
