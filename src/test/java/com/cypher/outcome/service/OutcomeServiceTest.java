package com.cypher.outcome.service;

import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.outcome.api.dto.OutcomeRequest;
import com.cypher.outcome.domain.OutcomeType;
import com.cypher.outcome.repository.OutcomeRepository;
import com.cypher.shared.exception.AnalysisNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutcomeServiceTest {

    @Mock
    private OutcomeRepository outcomeRepository;

    @Mock
    private RiskAnalysisRepository riskAnalysisRepository;

    @InjectMocks
    private OutcomeService service;

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
}
