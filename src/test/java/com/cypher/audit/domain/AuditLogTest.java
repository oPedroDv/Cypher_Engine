package com.cypher.audit.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditLogTest {

    @Test
    void builderAppliesSystemActorAndSuccessDefaults() {
        AuditLog log = AuditLog.builder(AuditAction.INVOICE_RECEIVED).build();

        assertThat(log.getId()).isNotNull();
        assertThat(log.getActorId()).isEqualTo("SYSTEM");
        assertThat(log.getActorType()).isEqualTo("SYSTEM");
        assertThat(log.isSuccess()).isTrue();
        assertThat(log.getOccurredAt()).isNotNull();
        assertThat(log.getErrorMessage()).isNull();
    }

    @Test
    void builderStoresEveryProvidedField() {
        UUID id = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2024-05-01T10:15:30Z");

        AuditLog log = AuditLog.builder(AuditAction.RISK_SCORE_CALCULATED)
                .id(id)
                .description("score calculado")
                .entity("RiskAnalysis", "analysis-1")
                .actor("user-1", "API")
                .correlationId("corr-1")
                .tenantId("tenant-1")
                .sourceIp("10.0.0.1")
                .httpEndpoint("POST /api/v1/analyses")
                .metadata(Map.of("score", "0.42"))
                .occurredAt(occurredAt)
                .build();

        assertThat(log.getId()).isEqualTo(id);
        assertThat(log.getAction()).isEqualTo(AuditAction.RISK_SCORE_CALCULATED);
        assertThat(log.getDescription()).isEqualTo("score calculado");
        assertThat(log.getEntityType()).isEqualTo("RiskAnalysis");
        assertThat(log.getEntityId()).isEqualTo("analysis-1");
        assertThat(log.getActorId()).isEqualTo("user-1");
        assertThat(log.getActorType()).isEqualTo("API");
        assertThat(log.getCorrelationId()).isEqualTo("corr-1");
        assertThat(log.getTenantId()).isEqualTo("tenant-1");
        assertThat(log.getSourceIp()).isEqualTo("10.0.0.1");
        assertThat(log.getHttpEndpoint()).isEqualTo("POST /api/v1/analyses");
        assertThat(log.getMetadata()).containsEntry("score", "0.42");
        assertThat(log.getOccurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void failureClearsSuccessFlag() {
        AuditLog log = AuditLog.builder(AuditAction.ANALYSIS_FAILED)
                .success(true)
                .failure("erro de integração")
                .build();

        assertThat(log.isSuccess()).isFalse();
        assertThat(log.getErrorMessage()).isEqualTo("erro de integração");
    }

    @Test
    void rejectsNullActionAndNullId() {
        assertThatThrownBy(() -> AuditLog.builder(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> AuditLog.builder(AuditAction.AUTH_LOGOUT).id(null).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id não pode ser nulo");
    }

    @Test
    void equalityIsBasedOnIdOnly() {
        UUID id = UUID.randomUUID();
        AuditLog first = AuditLog.builder(AuditAction.AUTH_LOGIN_SUCCESS).id(id).build();
        AuditLog second = AuditLog.builder(AuditAction.AUTH_LOGIN_FAILURE).id(id).build();
        AuditLog other = AuditLog.builder(AuditAction.AUTH_LOGIN_SUCCESS).build();

        assertThat(first).isEqualTo(first)
                .isEqualTo(second)
                .isNotEqualTo(other)
                .isNotEqualTo("not an audit log")
                .hasSameHashCodeAs(second);
    }

    @Test
    void toStringSummarizesEntityAndActor() {
        AuditLog log = AuditLog.builder(AuditAction.OUTCOME_REGISTERED)
                .entity("Outcome", "analysis-9")
                .actor("user-2", "API")
                .build();

        assertThat(log.toString())
                .contains("OUTCOME_REGISTERED")
                .contains("Outcome/analysis-9")
                .contains("user-2");
    }

    @Test
    void everyActionHasDescription() {
        assertThat(AuditAction.values()).allSatisfy(action ->
                assertThat(action.getDescription()).isNotBlank());
    }
}
