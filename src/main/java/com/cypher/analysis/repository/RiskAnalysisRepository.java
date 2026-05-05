package com.cypher.analysis.repository;

import com.cypher.analysis.domain.RiskAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RiskAnalysisRepository  extends JpaRepository<RiskAnalysis, UUID> {

    Optional<RiskAnalysis> findTopByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);
}