package com.cypher.analysis.adapter.persistence;

import com.cypher.analysis.application.port.RiskAnalysisPersistencePort;
import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class RiskAnalysisJpaAdapter implements RiskAnalysisPersistencePort {

    private final RiskAnalysisRepository repository;

    public RiskAnalysisJpaAdapter(RiskAnalysisRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<RiskAnalysis> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<RiskAnalysis> findTopByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId) {
        return repository.findTopByInvoiceIdOrderByCreatedAtDesc(invoiceId);
    }

    @Override
    public RiskAnalysis save(RiskAnalysis analysis) {
        return repository.save(analysis);
    }
}
