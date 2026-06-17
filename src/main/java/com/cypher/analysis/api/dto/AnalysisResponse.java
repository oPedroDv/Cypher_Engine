package com.cypher.analysis.api.dto;

import com.cypher.analysis.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AnalysisResponse(

        UUID analysisId,
        UUID invoiceId,

        boolean idempotent,

        double score,
        RiskLevel riskLevel,
        String recommendation,
        String modelVersion,

        boolean dataIsPartial,
        List<ScoreAdjustmentDto> scoreAdjustments,
        List<FactorDto> factors,
        FinancialDto financial,

        Instant createdAt

) {
    public static AnalysisResponse from(RiskAnalysis analysis, boolean idempotent) {
        RiskScore riskScore = RiskScore.of(analysis.getScore());

        List<FactorDto> factors = analysis.getFactors() != null
                ? analysis.getFactors().stream().map(FactorDto::from).toList()
                : List.of();

        FinancialDto financial = analysis.getFinancialMetrics() != null
                ? FinancialDto.from(analysis.getFinancialMetrics())
                : null;

        return new AnalysisResponse(
                analysis.getId(),
                analysis.getInvoice().getId(),
                idempotent,
                analysis.getScore(),
                riskScore.level(),
                resolveRecommendation(riskScore.level(), analysis.isDataPartial()),
                analysis.getModelVersion(),
                analysis.isDataPartial(),
                resolveScoreAdjustments(analysis.getScore(), factors),
                factors,
                financial,
                analysis.getCreatedAt()
        );
    }

    private static String resolveRecommendation(RiskLevel level, boolean partial) {
        if (partial) {
            return switch (level) {
                case LOW, MEDIUM -> "Dados parciais. Valide as fontes indisponíveis antes de aprovar a antecipação.";
                case HIGH -> "Exposição relevante com dados parciais. Considere taxa ajustada, garantias adicionais ou revisão manual.";
                case CRITICAL -> "Risco elevado com dados parciais. Não recomendado antecipar sem análise manual.";
            };
        }
        return switch (level) {
            case LOW      -> "Perfil favorável. Antecipação recomendada.";
            case MEDIUM   -> "Atenção recomendada. Verifique histórico do sacado.";
            case HIGH     -> "Exposição relevante. Considere taxa ajustada ou garantias adicionais.";
            case CRITICAL -> "Risco elevado. Não recomendado antecipar sem análise manual.";
        };
    }

    private static List<ScoreAdjustmentDto> resolveScoreAdjustments(double finalScore, List<FactorDto> factors) {
        double weightedScore = factors.stream()
                .mapToDouble(FactorDto::contribution)
                .sum();

        if (finalScore <= weightedScore + 0.000001) {
            return List.of();
        }

        return List.of(new ScoreAdjustmentDto(
                "ESCALATION_FLOOR",
                round(weightedScore),
                round(finalScore),
                "Score final elevado por piso de segurança da engine."
        ));
    }

    private static double round(double value) {
        return Math.round(value * 10_000d) / 10_000d;
    }

    public record ScoreAdjustmentDto(
            String type,
            double from,
            double to,
            String reason
    ) {}

    public record FactorDto(
            String name,
            String category,
            double score,
            double weight,
            double contribution,
            String direction,
            String explanation,
            String dataSource,
            boolean isFallback
    ) {
        public static FactorDto from(RiskFactor f) {
            return new FactorDto(
                    f.name(), f.category(), f.score(), f.weight(),
                    f.contribution(), f.direction(), f.explanation(),
                    f.dataSource(), f.isFallback()
            );
        }
    }

    public record FinancialDto(
            BigDecimal faceValue,
            BigDecimal requestedAdvanceValue,
            double expectedLossPct,
            double riskAdjustedRoiPct,
            BigDecimal maxAdvanceSuggested,
            double suggestedMonthlyRatePct,
            double advanceRatio,
            boolean isViable
    ) {
        public static FinancialDto from(FinancialMetrics m) {
            return new FinancialDto(
                    m.faceValue(),
                    m.requestedAdvanceValue(),
                    m.expectedLossPct(),
                    m.riskAdjustedRoiPct(),
                    m.maxAdvanceSuggested(),
                    m.suggestedMonthlyRatePct(),
                    m.advanceRatio(),
                    m.isViable()
            );
        }
    }
}
