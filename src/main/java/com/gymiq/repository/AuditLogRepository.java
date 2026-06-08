package com.gymiq.repository;

import com.gymiq.entity.AuditLog;
import com.gymiq.dto.response.AuditActorOptionResponse;
import com.gymiq.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByActorUserId(UUID actorUserId, Pageable pageable);

    Page<AuditLog> findByResourceTypeAndResourceId(ResourceType resourceType, String resourceId, Pageable pageable);

    @Query("""
            SELECT DISTINCT new com.gymiq.dto.response.AuditActorOptionResponse(
                a.actorUserId,
                a.actorEmail,
                a.actorRole
            )
            FROM AuditLog a
            WHERE a.actorUserId IS NOT NULL
            ORDER BY a.actorEmail ASC
            """)
    List<AuditActorOptionResponse> findActorOptions();
}
