package com.cypher.analysis.repository;

import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.domain.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskAnalysisRepository extends JpaRepository<RiskAnalysis, UUID> {

    Optional<RiskAnalysis> findTopByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);

    Page<RiskAnalysis> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<RiskAnalysis> findByRiskLevelOrderByCreatedAtDesc(RiskLevel riskLevel, Pageable pageable);

    long countByRiskLevel(RiskLevel riskLevel);

    @Query("SELECT COALESCE(AVG(r.score), 0.0) FROM RiskAnalysis r")
    double averageScore();

    @Query(value = """
            SELECT COALESCE(AVG((ra.financial_metrics ->> 'faceValue')::numeric), 0)
            FROM risk_analysis ra
            INNER JOIN invoice i ON i.id = ra.invoice_id
            WHERE i.issuer_cnpj = :issuerCnpj
              AND ra.financial_metrics IS NOT NULL
            """, nativeQuery = true)
    BigDecimal averageFaceValueByIssuerCnpj(@Param("issuerCnpj") String issuerCnpj);

    @Query(value = """
            SELECT TO_CHAR(created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD') AS day,
                   COUNT(*) AS cnt
            FROM risk_analysis
            WHERE created_at >= NOW() - INTERVAL '30 days'
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> countPerDayLast30Days();
}
