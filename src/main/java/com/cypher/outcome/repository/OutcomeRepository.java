package com.cypher.outcome.repository;

import com.cypher.outcome.domain.Outcome;
import com.cypher.outcome.domain.OutcomeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutcomeRepository extends JpaRepository<Outcome, UUID> {

    List<Outcome> findByAnalysisId(UUID analysisId);

    boolean existsByAnalysisIdAndTenantId(UUID analysisId, UUID tenantId);

    @Query("""
            SELECT o FROM Outcome o
            WHERE o.tenantId = :tenantId
            AND o.outcomeType = :outcomeType
            ORDER BY o.createdAt DESC
            """)
    List<Outcome> findByTenantIdAndOutcomeType(
            @Param("tenantId") UUID tenantId,
            @Param("outcomeType") OutcomeType outcomeType
    );

    @Query("""
            SELECT o FROM Outcome o
            INNER JOIN RiskAnalysis ra ON ra.id = o.analysisId
            INNER JOIN Invoice i ON i.id = ra.invoice.id
            WHERE i.issuerCnpj = :cnpj
            AND o.tenantId = :tenantId
            ORDER BY o.eventDate DESC
            """)
    List<Outcome> findByIssuerCnpj(
            @Param("cnpj") String cnpj,
            @Param("tenantId") UUID tenantId
    );

    @Query("""
            SELECT o FROM Outcome o
            INNER JOIN RiskAnalysis ra ON ra.id = o.analysisId
            INNER JOIN Invoice i ON i.id = ra.invoice.id
            WHERE i.payerCnpj = :cnpj
            AND o.tenantId = :tenantId
            ORDER BY o.eventDate DESC
            """)
    List<Outcome> findByPayerCnpj(
            @Param("cnpj") String cnpj,
            @Param("tenantId") UUID tenantId
    );
}