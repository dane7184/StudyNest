package com.client.lms.dashboard.repository;

import java.time.Instant;
import java.util.UUID;

import com.client.lms.dashboard.entity.AuditAction;
import com.client.lms.dashboard.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("""
            select a from AuditLog a
            where (:action is null or a.action = :action)
              and (:actor is null or a.actorKeycloakId = :actor)
              and (:targetId is null or a.targetId = :targetId)
              and (:from is null or a.createdAt >= :from)
              and (:to is null or a.createdAt <= :to)
            """)
    Page<AuditLog> search(@Param("action") AuditAction action,
                          @Param("actor") String actorKeycloakId,
                          @Param("targetId") String targetId,
                          @Param("from") Instant from,
                          @Param("to") Instant to,
                          Pageable pageable);
}
