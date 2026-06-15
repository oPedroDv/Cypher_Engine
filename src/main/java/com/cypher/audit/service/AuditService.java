package com.cypher.audit.service;

import com.cypher.audit.domain.AuditAction;
import com.cypher.audit.domain.AuditLog;
import com.cypher.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLog auditLog) {
        persist(auditLog);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(
            AuditAction action,
            String entityType,
            String entityId,
            String description,
            String correlationId,
            UUID tenantId
    ) {
        persist(
                AuditLog.builder(action)
                        .entity(entityType, entityId)
                        .description(description)
                        .correlationId(correlationId)
                        .tenantId(tenantId != null ? tenantId.toString() : null)
                        .success(true)
                        .build()
        );
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            AuditAction action,
            String entityType,
            String entityId,
            String description,
            String errorMessage,
            String correlationId,
            UUID tenantId
    ) {
        persist(
                AuditLog.builder(action)
                        .entity(entityType, entityId)
                        .description(description)
                        .failure(errorMessage)
                        .correlationId(correlationId)
                        .tenantId(tenantId != null ? tenantId.toString() : null)
                        .build()
        );
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordWithMetadata(
            AuditAction action,
            String entityType,
            String entityId,
            boolean success,
            String actorId,
            String actorType,
            String correlationId,
            UUID tenantId,
            Map<String, String> metadata
    ) {
        persist(
                AuditLog.builder(action)
                        .entity(entityType, entityId)
                        .actor(actorId, actorType)
                        .correlationId(correlationId)
                        .tenantId(tenantId != null ? tenantId.toString() : null)
                        .success(success)
                        .metadata(metadata)
                        .build()
        );
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSystemEvent(AuditAction action, String description) {
        persist(
                AuditLog.builder(action)
                        .actor("SYSTEM", "SYSTEM")
                        .description(description)
                        .occurredAt(Instant.now())
                        .build()
        );
    }

    private void persist(AuditLog auditLog) {
        try {
            repository.save(auditLog);
            log.debug("[AUDIT] {} | entity={}/{} | tenant={} | actor={} | success={}",
                    auditLog.getAction(),
                    auditLog.getEntityType(),
                    auditLog.getEntityId(),
                    auditLog.getTenantId(),
                    auditLog.getActorId(),
                    auditLog.isSuccess());
        } catch (Exception e) {
            log.error("[AUDIT] Falha ao persistir evento {} para entidade {}/{}: {}",
                    auditLog.getAction(),
                    auditLog.getEntityType(),
                    auditLog.getEntityId(),
                    e.getMessage(), e);
        }
    }
}
