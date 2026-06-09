package com.gymiq.controller;

import com.gymiq.dto.response.AuditFilterOptionsResponse;
import com.gymiq.dto.response.AuditLogResponse;
import com.gymiq.entity.AuditLog;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.exception.InvalidParameterException;
import com.gymiq.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void filterShouldNormalizeEnumParamsAndReturnPagedLogs() {
        AuditLogController controller = new AuditLogController(auditLogRepository);
        AuditLog log = auditLog();

        when(auditLogRepository.findAll(any(Specification.class), eq(Pageable.unpaged())))
                .thenReturn(new PageImpl<>(List.of(log)));

        ResponseEntity<Page<AuditLogResponse>> response = controller.filter(
                log.getActorUserId(),
                "login",
                "user",
                log.getResourceId(),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                Pageable.unpaged());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).getAction()).isEqualTo(AuditAction.LOGIN);
        verify(auditLogRepository).findAll(any(Specification.class), eq(Pageable.unpaged()));
    }

    @Test
    void filterShouldRejectUnknownAction() {
        AuditLogController controller = new AuditLogController(auditLogRepository);

        assertThatThrownBy(() -> controller.filter(null, "acao-inexistente", null, null, null, null, Pageable.unpaged()))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("filter-options");
    }

    @Test
    void filterShouldRejectUnknownResourceType() {
        AuditLogController controller = new AuditLogController(auditLogRepository);

        assertThatThrownBy(() -> controller.filter(null, null, "tipo-inexistente", null, null, null, Pageable.unpaged()))
                .isInstanceOf(InvalidParameterException.class)
                .hasMessageContaining("filter-options");
    }

    @Test
    void findFilterOptionsShouldReturnActionsResourceTypesAndActors() {
        AuditLogController controller = new AuditLogController(auditLogRepository);
        AuditLog first = auditLog();
        AuditLog duplicatedActor = auditLog();
        duplicatedActor.setAuditLogId(2L);
        AuditLog second = auditLog();
        second.setAuditLogId(3L);
        second.setActorUserId(UUID.fromString("00000000-0000-0000-0000-000000000031"));
        second.setActorEmail("recepcao@gymiq.com");
        second.setActorRole("RECEPTION");

        when(auditLogRepository.findByActorUserIdIsNotNullOrderByCreatedAtDesc())
                .thenReturn(List.of(first, duplicatedActor, second));

        ResponseEntity<AuditFilterOptionsResponse> response = controller.findFilterOptions();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getActions()).extracting("value").contains("LOGIN");
        assertThat(response.getBody().getResourceTypes()).extracting("value").contains("USER");
        assertThat(response.getBody().getActors()).hasSize(2);
        assertThat(response.getBody().getActors()).extracting("actorEmail")
                .containsExactly("admin@gymiq.com", "recepcao@gymiq.com");
    }

    @Test
    void findByActorShouldDelegateToRepository() {
        AuditLogController controller = new AuditLogController(auditLogRepository);
        AuditLog log = auditLog();

        when(auditLogRepository.findByActorUserId(log.getActorUserId(), Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(log)));

        ResponseEntity<Page<AuditLogResponse>> response = controller.findByActor(log.getActorUserId(), Pageable.unpaged());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTotalElements()).isEqualTo(1);
        verify(auditLogRepository).findByActorUserId(log.getActorUserId(), Pageable.unpaged());
    }

    @Test
    void findByResourceShouldNormalizeResourceTypeAndDelegateToRepository() {
        AuditLogController controller = new AuditLogController(auditLogRepository);
        AuditLog log = auditLog();

        when(auditLogRepository.findByResourceTypeAndResourceId(ResourceType.USER, log.getResourceId(), Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(log)));

        ResponseEntity<Page<AuditLogResponse>> response = controller.findByResource("user", log.getResourceId(), Pageable.unpaged());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent().get(0).getResourceType()).isEqualTo(ResourceType.USER);
        ArgumentCaptor<ResourceType> resourceTypeCaptor = ArgumentCaptor.forClass(ResourceType.class);
        verify(auditLogRepository).findByResourceTypeAndResourceId(resourceTypeCaptor.capture(), eq(log.getResourceId()), eq(Pageable.unpaged()));
        assertThat(resourceTypeCaptor.getValue()).isEqualTo(ResourceType.USER);
    }

    private AuditLog auditLog() {
        UUID actorId = UUID.fromString("00000000-0000-0000-0000-000000000030");
        return AuditLog.builder()
                .auditLogId(1L)
                .actorUserId(actorId)
                .actorEmail("admin@gymiq.com")
                .actorRole("ADMIN")
                .action(AuditAction.LOGIN)
                .resourceType(ResourceType.USER)
                .resourceId(actorId.toString())
                .description("Realizou login")
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
