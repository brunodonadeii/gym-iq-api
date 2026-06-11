package com.gymiq.repository;

import com.gymiq.entity.AuditLog;
import com.gymiq.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByActorUserId(UUID actorUserId, Pageable pageable);

    Page<AuditLog> findByResourceTypeAndResourceId(ResourceType resourceType, String resourceId, Pageable pageable);

    List<AuditLog> findByActorUserIdIsNotNullOrderByCreatedAtDesc();
}
