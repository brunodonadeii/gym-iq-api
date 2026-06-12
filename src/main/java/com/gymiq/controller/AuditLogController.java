package com.gymiq.controller;

import com.gymiq.dto.response.AuditActorOptionResponse;
import com.gymiq.dto.response.AuditFilterOptionResponse;
import com.gymiq.dto.response.AuditFilterOptionsResponse;
import com.gymiq.dto.response.AuditLogResponse;
import com.gymiq.entity.AuditLog;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.exception.InvalidParameterException;
import com.gymiq.repository.AuditLogRepository;
import com.gymiq.service.AuditLogResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogResponseService auditLogResponseService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> filter(
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AuditLog> logs = auditLogRepository.findAll(buildSpecification(
                        actorUserId,
                        resolveAuditAction(action),
                        resolveResourceType(resourceType),
                        resourceId,
                        from,
                        to), pageable);
        return ResponseEntity.ok(auditLogResponseService.toResponsePage(logs));
    }

    @GetMapping("/filter-options")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditFilterOptionsResponse> findFilterOptions() {
        return ResponseEntity.ok(new AuditFilterOptionsResponse(
                buildActionOptions(),
                buildResourceTypeOptions(),
                buildActorOptions()));
    }

    private Specification<com.gymiq.entity.AuditLog> buildSpecification(
            UUID actorUserId,
            AuditAction action,
            ResourceType resourceType,
            String resourceId,
            LocalDateTime from,
            LocalDateTime to) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (actorUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("actorUserId"), actorUserId));
            }
            if (action != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }
            if (resourceType != null) {
                predicates.add(criteriaBuilder.equal(root.get("resourceType"), resourceType));
            }
            if (resourceId != null) {
                predicates.add(criteriaBuilder.equal(root.get("resourceId"), resourceId));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private AuditAction resolveAuditAction(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalizedValue = normalizeEnumValue(value);
        return Arrays.stream(AuditAction.values())
                .filter(action -> action.name().equals(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new InvalidParameterException(
                        "Parâmetro inválido: action deve ser uma das opções disponíveis em /api/audit-logs/filter-options"));
    }

    private ResourceType resolveResourceType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return resolveRequiredResourceType(value);
    }

    private ResourceType resolveRequiredResourceType(String value) {
        String normalizedValue = normalizeEnumValue(value);
        return Arrays.stream(ResourceType.values())
                .filter(resourceType -> resourceType.name().equals(normalizedValue))
                .findFirst()
                .orElseThrow(() -> new InvalidParameterException(
                        "Parâmetro inválido: resourceType deve ser uma das opções disponíveis em /api/audit-logs/filter-options"));
    }

    private String normalizeEnumValue(String value) {
        return value.trim()
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase();
    }

    private List<AuditFilterOptionResponse> buildActionOptions() {
        return Arrays.stream(AuditAction.values())
                .map(action -> new AuditFilterOptionResponse(action.name(), action.getLabel()))
                .toList();
    }

    private List<AuditFilterOptionResponse> buildResourceTypeOptions() {
        return Arrays.stream(ResourceType.values())
                .map(resourceType -> new AuditFilterOptionResponse(resourceType.name(), resourceType.getLabel()))
                .toList();
    }

    private List<AuditActorOptionResponse> buildActorOptions() {
        Map<UUID, AuditActorOptionResponse> uniqueActors = new LinkedHashMap<>();

        for (AuditLog auditLog : auditLogRepository.findByActorUserIdIsNotNullOrderByCreatedAtDesc()) {
            uniqueActors.computeIfAbsent(
                    auditLog.getActorUserId(),
                    ignored -> new AuditActorOptionResponse(
                            auditLog.getActorUserId(),
                            auditLog.getActorEmail(),
                            auditLog.getActorRole()));
        }

        return uniqueActors.values().stream()
                .sorted(Comparator.comparing(
                        option -> option.getActorEmail() == null
                                ? ""
                                : option.getActorEmail().toLowerCase(Locale.ROOT)))
                .toList();
    }
}
