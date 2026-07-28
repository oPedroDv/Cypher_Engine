package com.cypher.audit.service;

import com.cypher.audit.domain.AuditAction;
import com.cypher.audit.domain.AuditLog;
import com.cypher.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditService service;

    @Test
    void recordPersistsProvidedLog() {
        AuditLog auditLog = AuditLog.builder(AuditAction.ANALYSIS_CREATED).build();

        service.record(auditLog);

        verify(repository).save(auditLog);
    }

    @Test
    void recordSuccessBuildsSuccessfulLogWithTenantAsString() {
        UUID tenantId = UUID.randomUUID();

        service.recordSuccess(AuditAction.ANALYSIS_COMPLETED, "RiskAnalysis", "analysis-1",
                "concluída", "corr-1", tenantId);

        AuditLog saved = capture();
        assertThat(saved.getAction()).isEqualTo(AuditAction.ANALYSIS_COMPLETED);
        assertThat(saved.getEntityType()).isEqualTo("RiskAnalysis");
        assertThat(saved.getEntityId()).isEqualTo("analysis-1");
        assertThat(saved.getDescription()).isEqualTo("concluída");
        assertThat(saved.getCorrelationId()).isEqualTo("corr-1");
        assertThat(saved.getTenantId()).isEqualTo(tenantId.toString());
        assertThat(saved.isSuccess()).isTrue();
        assertThat(saved.getOccurredAt()).isNotNull();
    }

    @Test
    void recordFailureStoresErrorMessageAndMarksFailure() {
        service.recordFailure(AuditAction.ANALYSIS_FAILED, "RiskAnalysis", "analysis-2",
                "falhou", "timeout na SEFAZ", "corr-2", null);

        AuditLog saved = capture();
        assertThat(saved.isSuccess()).isFalse();
        assertThat(saved.getErrorMessage()).isEqualTo("timeout na SEFAZ");
        assertThat(saved.getTenantId()).isNull();
    }

    @Test
    void recordWithMetadataStoresActorAndMetadata() {
        UUID tenantId = UUID.randomUUID();

        service.recordWithMetadata(AuditAction.OUTCOME_DUPLICATE_ATTEMPT, "Outcome", "analysis-3",
                false, "user-1", "API", "corr-3", tenantId, Map.of("requestedOutcome", "PAID"));

        AuditLog saved = capture();
        assertThat(saved.getActorId()).isEqualTo("user-1");
        assertThat(saved.getActorType()).isEqualTo("API");
        assertThat(saved.isSuccess()).isFalse();
        assertThat(saved.getMetadata()).containsEntry("requestedOutcome", "PAID");
    }

    @Test
    void recordSystemEventUsesSystemActor() {
        service.recordSystemEvent(AuditAction.SYSTEM_STARTUP, "aplicação iniciada");

        AuditLog saved = capture();
        assertThat(saved.getActorId()).isEqualTo("SYSTEM");
        assertThat(saved.getActorType()).isEqualTo("SYSTEM");
        assertThat(saved.getDescription()).isEqualTo("aplicação iniciada");
        assertThat(saved.isSuccess()).isTrue();
    }

    @Test
    void persistenceFailureDoesNotPropagate() {
        doThrow(new RuntimeException("db indisponível")).when(repository).save(any(AuditLog.class));

        assertThatCode(() -> service.recordSystemEvent(AuditAction.SYSTEM_SHUTDOWN, "encerrando"))
                .doesNotThrowAnyException();
    }

    private AuditLog capture() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
