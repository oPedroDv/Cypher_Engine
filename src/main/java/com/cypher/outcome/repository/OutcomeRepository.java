package com.cypher.outcome.repository;

import com.cypher.outcome.domain.Outcome;
import com.cypher.outcome.domain.OutcomeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutcomeRepository extends JpaRepository<Outcome, UUID> {

    List<Outcome> findByTenantIdAndAnalysisId(UUID tenantId, UUID analysisId);

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
            AND ra.tenantId = :tenantId
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
            WHERE i.recipientCnpj = :cnpj
            AND o.tenantId = :tenantId
            AND ra.tenantId = :tenantId
            ORDER BY o.eventDate DESC
            """)
    List<Outcome> findByPayerCnpj(
            @Param("cnpj") String cnpj,
            @Param("tenantId") UUID tenantId
    );

    @Query("""
            SELECT COUNT(o) FROM Outcome o
            INNER JOIN RiskAnalysis ra ON ra.id = o.analysisId
            INNER JOIN Invoice i ON i.id = ra.invoice.id
            WHERE i.issuerCnpj = :cnpj
            AND o.tenantId = :tenantId
            AND ra.tenantId = :tenantId
            AND o.outcomeType IN :outcomeTypes
            """)
    int countByIssuerCnpjAndOutcomeTypes(
            @Param("cnpj") String cnpj,
            @Param("tenantId") UUID tenantId,
            @Param("outcomeTypes") Collection<OutcomeType> outcomeTypes
    );

    @Query("""
            SELECT COUNT(o) FROM Outcome o
            INNER JOIN RiskAnalysis ra ON ra.id = o.analysisId
            INNER JOIN Invoice i ON i.id = ra.invoice.id
            WHERE i.recipientCnpj = :cnpj
            AND o.tenantId = :tenantId
            AND ra.tenantId = :tenantId
            AND o.outcomeType IN :outcomeTypes
            """)
    int countByPayerCnpjAndOutcomeTypes(
            @Param("cnpj") String cnpj,
            @Param("tenantId") UUID tenantId,
            @Param("outcomeTypes") Collection<OutcomeType> outcomeTypes
    );

    @Query("""
            SELECT COUNT(o) FROM Outcome o
            INNER JOIN RiskAnalysis ra ON ra.id = o.analysisId
            INNER JOIN Invoice i ON i.id = ra.invoice.id
            WHERE i.issuerCnpj = :issuerCnpj
            AND i.recipientCnpj = :recipientCnpj
            AND o.tenantId = :tenantId
            AND ra.tenantId = :tenantId
            AND o.outcomeType IN :outcomeTypes
            """)
    int countByPairAndOutcomeTypes(
            @Param("issuerCnpj") String issuerCnpj,
            @Param("recipientCnpj") String recipientCnpj,
            @Param("tenantId") UUID tenantId,
            @Param("outcomeTypes") Collection<OutcomeType> outcomeTypes
    );

    @Query("""
            SELECT COUNT(o) FROM Outcome o
            INNER JOIN RiskAnalysis ra ON ra.id = o.analysisId
            INNER JOIN Invoice i ON i.id = ra.invoice.id
            WHERE i.recipientCnpj = :cnpj
            AND o.tenantId = :tenantId
            AND ra.tenantId = :tenantId
            AND o.daysLate > 0
            """)
    int countLateByPayerCnpj(
            @Param("cnpj") String cnpj,
            @Param("tenantId") UUID tenantId
    );

    @Query("""
            SELECT new com.cypher.outcome.repository.OutcomeHistoryStats(
                COALESCE(SUM(CASE WHEN i.issuerCnpj = :issuerCnpj AND o.outcomeType IN :defaultOutcomes THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN i.recipientCnpj = :payerCnpj AND o.outcomeType IN :defaultOutcomes THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN i.issuerCnpj = :issuerCnpj AND i.recipientCnpj = :payerCnpj AND o.outcomeType IN :defaultOutcomes THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN i.recipientCnpj = :payerCnpj AND o.daysLate > 0 THEN 1 ELSE 0 END), 0)
            )
            FROM Outcome o
            INNER JOIN RiskAnalysis ra ON ra.id = o.analysisId
            INNER JOIN Invoice i ON i.id = ra.invoice.id
            WHERE o.tenantId = :tenantId
              AND ra.tenantId = :tenantId
            """)
    OutcomeHistoryStats summarizeHistory(
            @Param("issuerCnpj") String issuerCnpj,
            @Param("payerCnpj") String payerCnpj,
            @Param("tenantId") UUID tenantId,
            @Param("defaultOutcomes") Collection<OutcomeType> defaultOutcomes
    );
}
