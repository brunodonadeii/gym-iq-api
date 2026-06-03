package com.gymiq.controller;

import com.gymiq.dto.response.AuditLogResponse;
import com.gymiq.enums.AuditAction;
import com.gymiq.enums.ResourceType;
import com.gymiq.repository.AuditLogRepository;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> filter(
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) ResourceType resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository
                .findAll(buildSpecification(actorUserId, action, resourceType, resourceId, from, to), pageable)
                .map(AuditLogResponse::fromEntity));
    }

    @GetMapping("/actor/{actorUserId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> findByActor(
            @PathVariable UUID actorUserId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository.findByActorUserId(actorUserId, pageable)
                .map(AuditLogResponse::fromEntity));
    }

    @GetMapping("/resource/{resourceType}/{resourceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> findByResource(
            @PathVariable ResourceType resourceType,
            @PathVariable String resourceId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogRepository
                .findByResourceTypeAndResourceId(resourceType, resourceId, pageable)
                .map(AuditLogResponse::fromEntity));
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
}
