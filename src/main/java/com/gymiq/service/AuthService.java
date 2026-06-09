package com.gymiq.service;

import com.gymiq.aop.Auditable;
import com.gymiq.dto.request.LoginRequest;
import com.gymiq.dto.response.AuthResponse;
import com.gymiq.entity.User;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.exception.BusinessException;
import com.gymiq.repository.UserRepository;
import com.gymiq.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final PersonalDataProtectionService personalDataProtectionService;
    private final StudentContractService studentContractService;

    @Transactional(readOnly = true)
    @Auditable(action = AuditAction.LOGIN, resourceType = ResourceType.USER, description = "Realizou login")
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmailHash(personalDataProtectionService.emailHash(request.getEmail()))
                .orElseThrow(() -> new BusinessException("Usuário não encontrado"));

        if (Boolean.FALSE.equals(user.getActive())) {
            throw new BusinessException("Usuário inativo");
        }

        studentContractService.validateStudentLoginAccess(user);

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

}
