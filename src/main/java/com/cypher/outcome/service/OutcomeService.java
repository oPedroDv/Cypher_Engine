package com.cypher.outcome.service;

import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.outcome.api.dto.OutcomeRequest;
import com.cypher.outcome.domain.Outcome;
import com.cypher.outcome.repository.OutcomeRepository;
import com.cypher.shared.exception.AnalysisNotFoundException;
import com.cypher.audit.domain.AuditAction;
import com.cypher.audit.service.AuditService;
import com.cypher.infrastructure.web.CorrelationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutcomeService {

    private final OutcomeRepository outcomeRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final AuditService auditService;


    @Transactional(readOnly = true)
    public void assertAnalysisAccess(UUID analysisId, UUID tenantId) {
        RiskAnalysis analysis = riskAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));
        if (!tenantId.equals(analysis.getTenantId())) {
            throw new AccessDeniedException("analysis belongs to another tenant");
        }
    }

    @Transactional
    public void register(UUID analysisId, OutcomeRequest request, UUID tenantId) {

        RiskAnalysis analysis = riskAnalysisRepository.findByIdAndTenantId(analysisId, tenantId)
                .orElseThrow(() -> new AnalysisNotFoundException(analysisId));


        if (outcomeRepository.existsByAnalysisIdAndTenantId(analysisId, tenantId)) {
            log.warn("Outcome já registrado para analysisId={} tenant={}", analysisId, tenantId);

            auditService.recordWithMetadata(AuditAction.OUTCOME_DUPLICATE_ATTEMPT,
                    "Outcome", analysisId.toString(), false, "SYSTEM", "API",
                    CorrelationContext.getOrCreate(), tenantId,
                    Map.of("requestedOutcome", request.outcome().name()));
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


        auditService.recordSuccess(AuditAction.OUTCOME_REGISTERED, "Outcome", analysisId.toString(),
                request.outcome().name(), CorrelationContext.getOrCreate(), tenantId);

        log.info("Outcome registrado analysisId={} type={} tenant={}",
                analysisId, request.outcome(), tenantId);
    }
}
