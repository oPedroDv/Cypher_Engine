package com.cypher.analysis.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.math.MathContext;
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

    private static final BigDecimal LOSS_MULTIPLIER = new BigDecimal("0.18");
    private static final BigDecimal ADVANCE_HAIRCUT = new BigDecimal("0.15");
    private static final BigDecimal MAX_ADVANCE_RATIO = new BigDecimal("0.95");
    private static final BigDecimal RATE_RISK_PREMIUM = new BigDecimal("2.50");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    public static FinancialMetrics calculate(
            BigDecimal faceValue,
            BigDecimal requestedAdvance,
            double requestedMonthlyRate,
            double riskScore
    ) {
        BigDecimal score    = BigDecimal.valueOf(riskScore);
        BigDecimal rate     = BigDecimal.valueOf(requestedMonthlyRate);
        BigDecimal advance  = requestedAdvance != null
                ? requestedAdvance
                : faceValue.multiply(new BigDecimal("0.90"), MC);

        BigDecimal expectedLoss = score.multiply(LOSS_MULTIPLIER, MC).multiply(ONE_HUNDRED, MC);

        BigDecimal riskAdjustedRoi = rate.subtract(expectedLoss, MC);

        BigDecimal haircut    = score.multiply(ADVANCE_HAIRCUT, MC);
        BigDecimal maxAdvance = faceValue
                .multiply(BigDecimal.ONE.subtract(haircut, MC), MC)
                .multiply(MAX_ADVANCE_RATIO, MC)
                .setScale(2, RoundingMode.HALF_DOWN);

        BigDecimal suggestedRate = rate.add(score.multiply(RATE_RISK_PREMIUM, MC), MC);

        BigDecimal ratio = faceValue.compareTo(BigDecimal.ZERO) > 0
                ? advance.divide(faceValue, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new FinancialMetrics(
                faceValue,
                requestedAdvance != null ? requestedAdvance : advance,
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