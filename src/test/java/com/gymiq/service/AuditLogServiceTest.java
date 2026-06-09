package com.gymiq.service;

import com.gymiq.entity.AuditLog;
import com.gymiq.entity.User;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.repository.AuditLogRepository;
import com.gymiq.repository.UserRepository;
import com.gymiq.support.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonalDataProtectionService personalDataProtectionService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordShouldPersistAuditLogWithAuthenticatedUserAndForwardedIp() {
        User admin = TestDataFactory.activeAdminUser();
        AuditLogService service = new AuditLogService(auditLogRepository, userRepository, personalDataProtectionService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(admin.getEmail(), "n/a", "ROLE_ADMIN"));

        when(personalDataProtectionService.emailHash(admin.getEmail())).thenReturn("admin-hash");
        when(userRepository.findByEmailHash("admin-hash")).thenReturn(Optional.of(admin));

        service.record(AuditAction.CREATE_USER, ResourceType.USER, admin.getUserId().toString(), "Criou usuario");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog auditLog = captor.getValue();
        assertThat(auditLog.getActorUserId()).isEqualTo(admin.getUserId());
        assertThat(auditLog.getActorEmail()).isEqualTo(admin.getEmail());
        assertThat(auditLog.getActorRole()).isEqualTo("ADMIN");
        assertThat(auditLog.getIpAddress()).isEqualTo("203.0.113.10");
    }

    @Test
    void recordShouldUseAuthenticationRoleWhenUserIsNotFound() {
        AuditLogService service = new AuditLogService(auditLogRepository, userRepository, personalDataProtectionService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "198.51.100.7");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(
                        "instrutor@gymiq.com",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))));

        when(personalDataProtectionService.emailHash("instrutor@gymiq.com")).thenReturn("hash");
        when(userRepository.findByEmailHash("hash")).thenReturn(Optional.empty());

        service.record(AuditAction.UPDATE_WORKOUT_SHEET, ResourceType.WORKOUT_SHEET, "sheet-id", "Atualizou ficha");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActorUserId()).isNull();
        assertThat(captor.getValue().getActorEmail()).isEqualTo("instrutor@gymiq.com");
        assertThat(captor.getValue().getActorRole()).isEqualTo("INSTRUCTOR");
        assertThat(captor.getValue().getIpAddress()).isEqualTo("198.51.100.7");
    }

    @Test
    void recordShouldPersistAnonymousAuditWhenThereIsNoAuthentication() {
        AuditLogService service = new AuditLogService(auditLogRepository, userRepository, personalDataProtectionService);

        service.record(AuditAction.GENERATE_RETENTION_ALERT, ResourceType.RETENTION_ALERT, null, "Gerou alertas");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActorUserId()).isNull();
        assertThat(captor.getValue().getActorEmail()).isNull();
        assertThat(captor.getValue().getIpAddress()).isNull();
    }

    @Test
    void recordShouldSwallowRepositoryFailures() {
        AuditLogService service = new AuditLogService(auditLogRepository, userRepository, personalDataProtectionService);
        doThrow(new RuntimeException("database offline")).when(auditLogRepository).save(any(AuditLog.class));

        service.record(AuditAction.LOGIN, ResourceType.USER, null, "Login");

        verify(auditLogRepository).save(any(AuditLog.class));
    }
}
