package com.cypher.outcome.service;

import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.audit.domain.AuditAction;
import com.cypher.audit.service.AuditService;
import com.cypher.infrastructure.web.CorrelationContext;
import com.cypher.outcome.api.dto.OutcomeRequest;
import com.cypher.outcome.domain.Outcome;
import com.cypher.outcome.domain.OutcomeType;
import com.cypher.outcome.repository.OutcomeRepository;
import com.cypher.shared.exception.AnalysisNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutcomeServiceTest {

    @Mock
    private OutcomeRepository outcomeRepository;

    @Mock
    private RiskAnalysisRepository riskAnalysisRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private OutcomeService service;

    @AfterEach
    void clearCorrelation() {
        CorrelationContext.clear();
    }

    @Test
    void shouldRejectOutcomeForAnalysisFromAnotherTenant() {
        UUID analysisId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        OutcomeRequest request = new OutcomeRequest(
                OutcomeType.PAID,
                LocalDate.now(),
                null,
                null,
                "ok"
        );

        when(riskAnalysisRepository.findByIdAndTenantId(analysisId, tenantId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(analysisId, request, tenantId))
                .isInstanceOf(AnalysisNotFoundException.class);

        verify(riskAnalysisRepository).findByIdAndTenantId(analysisId, tenantId);
        verify(riskAnalysisRepository, never()).findById(analysisId);
        verify(outcomeRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldPersistOutcomeAndAuditSuccess() {
        UUID analysisId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        OutcomeRequest request = new OutcomeRequest(
                OutcomeType.PARTIAL,
                LocalDate.now().minusDays(1),
                new BigDecimal("9500.00"),
                12,
                "pago com atraso"
        );
        CorrelationContext.set("corr-1");

        RiskAnalysis analysis = mock(RiskAnalysis.class);
        when(analysis.getId()).thenReturn(analysisId);
        when(riskAnalysisRepository.findByIdAndTenantId(analysisId, tenantId))
                .thenReturn(Optional.of(analysis));
        when(outcomeRepository.existsByAnalysisIdAndTenantId(analysisId, tenantId)).thenReturn(false);

        service.register(analysisId, request, tenantId);

        ArgumentCaptor<Outcome> captor = ArgumentCaptor.forClass(Outcome.class);
        verify(outcomeRepository).save(captor.capture());
        Outcome saved = captor.getValue();
        assertThat(saved.getAnalysisId()).isEqualTo(analysisId);
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getOutcomeType()).isEqualTo(OutcomeType.PARTIAL);
        assertThat(saved.getAmountReceived()).isEqualByComparingTo("9500.00");
        assertThat(saved.getDaysLate()).isEqualTo(12);
        assertThat(saved.getNotes()).isEqualTo("pago com atraso");

        verify(auditService).recordSuccess(AuditAction.OUTCOME_REGISTERED, "Outcome",
                analysisId.toString(), "PARTIAL", "corr-1", tenantId);
    }

    @Test
    void shouldSkipDuplicateOutcomeAndAuditAttempt() {
        UUID analysisId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        OutcomeRequest request = new OutcomeRequest(OutcomeType.PAID, LocalDate.now(), null, null, null);

        when(riskAnalysisRepository.findByIdAndTenantId(analysisId, tenantId))
                .thenReturn(Optional.of(mock(RiskAnalysis.class)));
        when(outcomeRepository.existsByAnalysisIdAndTenantId(analysisId, tenantId)).thenReturn(true);

        service.register(analysisId, request, tenantId);

        verify(outcomeRepository, never()).save(any());
        verify(auditService).recordWithMetadata(eq(AuditAction.OUTCOME_DUPLICATE_ATTEMPT), eq("Outcome"),
                eq(analysisId.toString()), eq(false), eq("SYSTEM"), eq("API"), any(), eq(tenantId),
                eq(Map.of("requestedOutcome", "PAID")));
    }

    @Test
    void assertAnalysisAccessAcceptsOwningTenant() {
        UUID analysisId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        RiskAnalysis analysis = mock(RiskAnalysis.class);
        when(analysis.getTenantId()).thenReturn(tenantId);
        when(riskAnalysisRepository.findById(analysisId)).thenReturn(Optional.of(analysis));

        assertThatCode(() -> service.assertAnalysisAccess(analysisId, tenantId))
                .doesNotThrowAnyException();
    }

    @Test
    void assertAnalysisAccessRejectsForeignTenant() {
        UUID analysisId = UUID.randomUUID();
        RiskAnalysis analysis = mock(RiskAnalysis.class);
        when(analysis.getTenantId()).thenReturn(UUID.randomUUID());
        when(riskAnalysisRepository.findById(analysisId)).thenReturn(Optional.of(analysis));

        assertThatThrownBy(() -> service.assertAnalysisAccess(analysisId, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assertAnalysisAccessThrowsWhenAnalysisIsAbsent() {
        UUID analysisId = UUID.randomUUID();
        when(riskAnalysisRepository.findById(analysisId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertAnalysisAccess(analysisId, UUID.randomUUID()))
                .isInstanceOf(AnalysisNotFoundException.class);
    }
}
