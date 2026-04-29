package com.cypher.analysis.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "risk_analysis")
public class RiskAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch =FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private double score;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @Column(name = "created_at", nullable = false, updatable =  false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }

    public static RiskAnalysis of (Invoice invoice, double score, String modelVersion) {
        RiskAnalysis analysis = new RiskAnalysis();
        analysis.invoice = invoice;
        analysis.score = score;
        analysis.riskLevel = RiskLevel.from(score);
        analysis.modelVersion = modelVersion;
        return analysis;
    }

    public UUID getId()             {return id;}
    public Invoice getInvoice()     {return invoice;}
    public double getScore()        {return score;}
    public RiskLevel getRiskLevel() {return riskLevel;}
    public String getModelVersion() {return modelVersion;}
    public Instant getCreatedAt()   {return createdAt;}
}

