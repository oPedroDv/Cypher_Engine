package com.cypher.audit.repository;

import com.cypher.audit.domain.AuditAction;
import com.cypher.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
            String entityType,
            String entityId,
            Pageable pageable
    );

    Page<AuditLog> findByEntityTypeAndEntityIdAndActionOrderByOccurredAtDesc(
            String entityType,
            String entityId,
            AuditAction action,
            Pageable pageable
    );

    Page<AuditLog> findByActionAndOccurredAtBetweenOrderByOccurredAtDesc(
            AuditAction action,
            Instant from,
            Instant to,
            Pageable pageable
    );

    List<AuditLog> findByCorrelationIdOrderByOccurredAtAsc(String correlationId);

    @Query("""
        SELECT COUNT(a) FROM AuditLog a
        WHERE a.action = :action
          AND a.entityType = 'INVOICE'
          AND a.actorId = :cnpj
          AND a.occurredAt >= :since
    """)
    long countByActionAndActorIdSince(
            @Param("action") AuditAction action,
            @Param("cnpj") String cnpj,
            @Param("since") Instant since
    );

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.action IN (
            com.cypher.audit.domain.AuditAction.AUTH_LOGIN_FAILURE,
            com.cypher.audit.domain.AuditAction.AUTH_PERMISSION_DENIED
        )
        AND a.actorId = :actorId
        AND a.occurredAt >= :since
        ORDER BY a.occurredAt DESC
    """)
    List<AuditLog> findRecentAuthFailures(
            @Param("actorId") String actorId,
            @Param("since") Instant since
    );

    boolean existsByCorrelationIdAndAction(String correlationId, AuditAction action);
}