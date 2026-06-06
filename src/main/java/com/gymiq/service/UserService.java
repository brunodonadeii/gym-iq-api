package com.gymiq.service;

import com.gymiq.aop.Auditable;
import com.gymiq.dto.request.CreateUserRequest;
import com.gymiq.dto.request.UpdateUserRequest;
import com.gymiq.dto.response.UserResponse;
import com.gymiq.entity.User;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.exception.BusinessException;
import com.gymiq.exception.ResourceNotFoundException;
import com.gymiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final List<User.Role> ADMINISTRATIVE_ROLES = List.of(
            User.Role.ADMIN,
            User.Role.RECEPTION);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonalDataProtectionService personalDataProtectionService;

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findByRoleIn(ADMINISTRATIVE_ROLES, pageable)
                .map(UserResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return UserResponse.fromEntity(findAdministrativeUser(id));
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE_USER, resourceType = ResourceType.USER, description = "Criou usuario administrativo")
    public UserResponse createAdministrativeUser(CreateUserRequest request) {
        validateAdministrativeRole(request.getRole());

        String emailHash = personalDataProtectionService.emailHash(request.getEmail());

        if (userRepository.existsByEmailHash(emailHash)) {
            throw new BusinessException("E-mail já cadastrado: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .emailHash(emailHash)
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

    @Transactional
    @Auditable(action = AuditAction.UPDATE_USER, resourceType = ResourceType.USER, description = "Atualizou usuario administrativo")
    public UserResponse updateAdministrativeUser(UUID id, UpdateUserRequest request) {
        validateAdministrativeRole(request.getRole());

        User user = findAdministrativeUser(id);

        String emailHash = personalDataProtectionService.emailHash(request.getEmail());

        userRepository.findByEmailHash(emailHash)
                .filter(existingUser -> !existingUser.getUserId().equals(id))
                .ifPresent(existingUser -> {
                    throw new BusinessException("E-mail já cadastrado: " + request.getEmail());
                });

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setEmailHash(emailHash);
        user.setRole(request.getRole());
        user.setLgpdAccepted(request.getLgpdAccepted());
        if (Boolean.TRUE.equals(request.getLgpdAccepted()) && user.getLgpdAcceptedAt() == null) {
            user.setLgpdAcceptedAt(LocalDateTime.now());
        }
        if (Boolean.FALSE.equals(request.getLgpdAccepted())) {
            user.setLgpdAcceptedAt(null);
        }

        userRepository.save(user);
        log.info("Usuario administrativo atualizado: id={}, role={}", user.getUserId(), user.getRole());
        return UserResponse.fromEntity(user);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE_USER, resourceType = ResourceType.USER, description = "Excluiu usuario administrativo")
    public void deleteAdministrativeUser(UUID id) {
        User user = findAdministrativeUser(id);
        userRepository.delete(user);
        log.info("Usuario administrativo removido: id={}, role={}", user.getUserId(), user.getRole());
    }

    private void validateAdministrativeRole(User.Role role) {
        if (role == User.Role.STUDENT || role == User.Role.INSTRUCTOR) {
            throw new BusinessException("Use as rotas específicas para criar alunos ou instrutores");
        }
    }

    private User findAdministrativeUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));

        validateAdministrativeRole(user.getRole());
        return user;
    }

    private LocalDateTime resolveLgpdAcceptedAt(Boolean lgpdAccepted) {
        return Boolean.TRUE.equals(lgpdAccepted) ? LocalDateTime.now() : null;
    }
}
