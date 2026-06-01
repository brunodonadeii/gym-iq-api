package com.gymiq.service;

import com.gymiq.entity.AuditLog;
import com.gymiq.entity.User;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.repository.AuditLogRepository;
import com.gymiq.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditAction action, ResourceType resourceType, Integer resourceId, String description) {
        try {
            ActorContext actor = resolveActor();

            AuditLog auditLog = AuditLog.builder()
                    .actorUserId(actor.userId())
                    .actorEmail(actor.email())
                    .actorRole(actor.role())
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .description(description)
                    .ipAddress(resolveIpAddress())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception exception) {
            log.error("Falha ao registrar auditoria: action={}, resourceType={}, resourceId={}, error={}",
                    action, resourceType, resourceId, exception.getMessage());
        }
    }

    private ActorContext resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ActorContext.empty();
        }

        String email = authentication.getName();
        if (email == null || email.isBlank() || "anonymousUser".equals(email)) {
            return ActorContext.empty();
        }

        return userRepository.findByEmail(email)
                .map(user -> new ActorContext(user.getUserId(), user.getEmail(), user.getRole().name()))
                .orElseGet(() -> new ActorContext(null, email, resolveRole(authentication)));
    }

    private String resolveRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }

    private String resolveIpAddress() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private record ActorContext(Integer userId, String email, String role) {
        static ActorContext empty() {
            return new ActorContext(null, null, null);
        }
    }
}
