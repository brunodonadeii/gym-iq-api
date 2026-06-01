package com.gymiq.repository;

import com.gymiq.entity.AuditLog;
import com.gymiq.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByActorUserId(Integer actorUserId, Pageable pageable);

    Page<AuditLog> findByResourceTypeAndResourceId(ResourceType resourceType, Integer resourceId, Pageable pageable);
}
