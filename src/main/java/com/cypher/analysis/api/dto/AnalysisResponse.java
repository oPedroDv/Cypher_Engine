package com.cypher.analysis.api.dto;

import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.domain.RiskLevel;

import java.time.Instant;
import java.util.UUID;

public record AnalysisResponse(
        UUID analysisId,
        UUID invoiceId,
        double score,
        RiskLevel riskLevel,
        String recommendation,
        String modelVersion,
        Instant createdAt
) {
    public static AnalysisResponse from(RiskAnalysis analysis) {
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getInvoice().getId(),
                analysis.getScore(),
                analysis.getRiskLevel(),
                resolveRecommendation(analysis.getRiskLevel()),
                analysis.getModelVersion(),
                analysis.getCreatedAt()
        );
    }
    private static String resolveRecommendation(RiskLevel level) {
        return switch (level) {
            case LOW -> "Perfil favorável. Antecipação recomendada.";
            case MEDIUM -> "Atenção recomendada. Verifique histórico do sacado.";
            case HIGH -> "Exposição relevante. Considere taxa ajustado ou garantias adicionais.";
            case CRITICAL -> "Risco elevado. Não recomendado antecipar sem analise manual.";
        };
    }
}