package com.cypher.analysis.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinancialMetrics(
        BigDecimal faceValue,
        BigDecimal requestedAdvanceValue,
        double expectedLossPct,
        double riskAdjustedRoiPct,
        BigDecimal maxAdvanceSuggested,
        double suggestedMonthlyRatePct,
        double advanceRatio
) {

    private static final double LOSS_MULTIPLIER   = 0.18;
    private static final double ADVANCE_HAIRCUT   = 0.15;
    private static final double MAX_ADVANCE_RATIO = 0.95;
    private static final double RATE_RISK_PREMIUM = 2.50;

    public static FinancialMetrics calculate(
            BigDecimal faceValue,
            BigDecimal requestedAdvance,
            double requestedMonthlyRate,
            double riskScore
    ) {
        double face    = faceValue.doubleValue();
        double advance = requestedAdvance != null ? requestedAdvance.doubleValue() : face * 0.90;

        double expectedLoss     = riskScore * LOSS_MULTIPLIER * 100;
        double riskAdjustedRoi  = requestedMonthlyRate - expectedLoss;
        double maxAdvanceDouble = face * (1 - riskScore * ADVANCE_HAIRCUT) * MAX_ADVANCE_RATIO;
        double suggestedRate    = requestedMonthlyRate + (riskScore * RATE_RISK_PREMIUM);
        double ratio            = face > 0 ? advance / face : 0.0;

        return new FinancialMetrics(
                faceValue,
                requestedAdvance != null ? requestedAdvance : faceValue,
                roundValue(expectedLoss),
                roundValue(riskAdjustedRoi),
                BigDecimal.valueOf(maxAdvanceDouble).setScale(2, RoundingMode.HALF_DOWN),
                roundValue(suggestedRate),
                roundValue(ratio)
        );
    }

    public boolean isViable() {
        return riskAdjustedRoiPct > 0;
    }

    public boolean isAdvanceWithinLimit() {
        return requestedAdvanceValue.compareTo(maxAdvanceSuggested) <= 0;
    }

    private static double roundValue(double value) {
        return Math.round(value * 10000) / 10000.0;
    }
}
