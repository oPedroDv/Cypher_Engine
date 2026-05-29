package com.cypher.outcome.service;

import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.outcome.api.dto.OutcomeRequest;
import com.cypher.outcome.domain.Outcome;
import com.cypher.outcome.repository.OutcomeRepository;
import com.cypher.shared.exception.AnalysisNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutcomeService {

    private final OutcomeRepository outcomeRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;

    @Transactional
    public void register(UUID analysisId, OutcomeRequest request, UUID tenantId) {

        RiskAnalysis analysis = riskAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));

        // Idempotência simples — evita duplicar outcome para a mesma análise
        if (outcomeRepository.existsByAnalysisIdAndTenantId(analysisId, tenantId)) {
            log.warn("Outcome já registrado para analysisId={} tenant={}", analysisId, tenantId);
            return;
        }

        Outcome outcome = Outcome.of(
                analysis.getId(),
                tenantId,
                request.outcome(),
                request.eventDate(),
                request.amountReceived(),
                request.daysLate(),
                request.notes()
        );

        outcomeRepository.save(outcome);

        log.info("Outcome registrado analysisId={} type={} tenant={}",
                analysisId, request.outcome(), tenantId);
    }
}