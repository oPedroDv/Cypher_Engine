package com.cypher.audit.service;

import com.cypher.audit.domain.AuditAction;
import com.cypher.audit.domain.AuditLog;
import com.cypher.audit.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }
    
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
            String correlationId
    ) {
        persist(
                AuditLog.builder(action)
                        .entity(entityType, entityId)
                        .description(description)
                        .correlationId(correlationId)
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
            String correlationId
    ) {
        persist(
                AuditLog.builder(action)
                        .entity(entityType, entityId)
                        .description(description)
                        .failure(errorMessage)
                        .correlationId(correlationId)
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
            Map<String, String> metadata
    ) {
        persist(
                AuditLog.builder(action)
                        .entity(entityType, entityId)
                        .actor(actorId, actorType)
                        .correlationId(correlationId)
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
            log.debug("[AUDIT] {} | entity={}/{} | actor={} | success={}",
                    auditLog.getAction(),
                    auditLog.getEntityType(),
                    auditLog.getEntityId(),
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