package com.cypher.analysis.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "risk_analysis",
        indexes = {
                @Index(name = "idx_risk_analysis_invoice_id",   columnList = "invoice_id"),
                @Index(name = "idx_risk_analysis_created_at",   columnList = "created_at DESC"),
                @Index(name = "idx_risk_analysis_risk_level",   columnList = "risk_level")
        }
)
public class RiskAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private double score;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "factors", columnDefinition = "jsonb")
    private List<RiskFactor> factors;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "financial_metrics", columnDefinition = "jsonb")
    private FinancialMetrics financialMetrics;

    @Column(name = "data_partial", nullable = false)
    private boolean dataPartial;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RiskAnalysis() {}

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }

    public static RiskAnalysis of(
            Invoice invoice,
            double score,
            String modelVersion,
            List<RiskFactor> factors,
            FinancialMetrics financialMetrics,
            boolean dataPartial
    ) {
        RiskAnalysis analysis = new RiskAnalysis();
        analysis.invoice          = invoice;
        analysis.score            = score;
        analysis.riskLevel        = RiskLevel.from(score);
        analysis.modelVersion     = modelVersion;
        analysis.factors          = factors;
        analysis.financialMetrics = financialMetrics;
        analysis.dataPartial      = dataPartial;
        return analysis;
    }

    public UUID getId() { return id; }
    public Invoice getInvoice() { return invoice; }
    public double getScore() { return score; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public String getModelVersion() { return modelVersion; }
    public List<RiskFactor> getFactors() { return factors; }
    public FinancialMetrics getFinancialMetrics(){ return financialMetrics; }
    public boolean isDataPartial() { return dataPartial; }
    public Instant getCreatedAt() { return createdAt; }
}