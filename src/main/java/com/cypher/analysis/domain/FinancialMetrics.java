package com.cypher.analysis.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

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

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    public static FinancialMetrics calculate(
            BigDecimal faceValue,
            BigDecimal requestedAdvance,
            double requestedMonthlyRate,
            double riskScore,
            BigDecimal lossMultiplier,
            BigDecimal advanceHaircut,
            BigDecimal maxAdvanceRatio,
            BigDecimal rateRiskPremium
    ) {
        BigDecimal score    = BigDecimal.valueOf(riskScore);
        BigDecimal rate     = BigDecimal.valueOf(requestedMonthlyRate);

        BigDecimal advance = Objects.requireNonNull(requestedAdvance, "requestedAdvance é obrigatório");

        BigDecimal expectedLoss = score.multiply(lossMultiplier, MC).multiply(ONE_HUNDRED, MC);

        BigDecimal riskAdjustedRoi = rate.subtract(expectedLoss, MC);

        BigDecimal haircut    = score.multiply(advanceHaircut, MC);
        BigDecimal maxAdvance = faceValue
                .multiply(BigDecimal.ONE.subtract(haircut, MC), MC)
                .multiply(maxAdvanceRatio, MC)
                .setScale(2, RoundingMode.HALF_DOWN);

        BigDecimal suggestedRate = rate.add(score.multiply(rateRiskPremium, MC), MC);

        BigDecimal ratio = faceValue.compareTo(BigDecimal.ZERO) > 0
                ? advance.divide(faceValue, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new FinancialMetrics(
                faceValue,
                advance,
                round4(expectedLoss),
                round4(riskAdjustedRoi),
                maxAdvance,
                round4(suggestedRate),
                ratio.doubleValue()
        );
    }

    public boolean isViable() {
        return riskAdjustedRoiPct > 0;
    }

    public boolean isAdvanceWithinLimit() {
        return requestedAdvanceValue.compareTo(maxAdvanceSuggested) <= 0;
    }

    private static double round4(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
