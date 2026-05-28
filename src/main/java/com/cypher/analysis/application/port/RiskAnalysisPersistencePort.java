package com.cypher.analysis.application.port;

import com.cypher.analysis.domain.RiskAnalysis;

import java.util.Optional;
import java.util.UUID;

public interface RiskAnalysisPersistencePort {

    Optional<RiskAnalysis> findById(UUID id);

    Optional<RiskAnalysis> findTopByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);

    RiskAnalysis save(RiskAnalysis analysis);
}
