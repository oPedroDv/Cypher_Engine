package com.cypher.analysis.service;

import com.cypher.analysis.domain.FinancialMetrics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialMetricsServiceTest {

    private final FinancialMetricsService service = new FinancialMetricsService();

    @Test
    void calculateUsesRequestedAdvanceWhenProvided() {
        FinancialMetrics metrics = service.calculate(
                new BigDecimal("10000.00"),
                new BigDecimal("8000.00"),
                2.5,
                0.2
        );

        assertThat(metrics.faceValue()).isEqualByComparingTo("10000.00");
        assertThat(metrics.requestedAdvanceValue()).isEqualByComparingTo("8000.00");
        assertThat(metrics.expectedLossPct()).isEqualTo(3.6);
        assertThat(metrics.riskAdjustedRoiPct()).isEqualTo(-1.1);
        assertThat(metrics.maxAdvanceSuggested()).isEqualByComparingTo("9215.00");
        assertThat(metrics.suggestedMonthlyRatePct()).isEqualTo(3.0);
        assertThat(metrics.advanceRatio()).isEqualTo(0.8);
        assertThat(metrics.isViable()).isFalse();
        assertThat(metrics.isAdvanceWithinLimit()).isTrue();
    }

    @Test
    void calculateDefaultsAdvanceToNinetyPercentOfFaceValue() {
        FinancialMetrics metrics = service.calculate(
                new BigDecimal("10000.00"),
                null,
                5.0,
                0.1
        );

        assertThat(metrics.requestedAdvanceValue()).isEqualByComparingTo("9000.000");
        assertThat(metrics.advanceRatio()).isEqualTo(0.9);
        assertThat(metrics.isViable()).isTrue();
    }

    @Test
    void calculateFlagsAdvanceAboveRiskAdjustedLimit() {
        FinancialMetrics metrics = service.calculate(
                new BigDecimal("10000.00"),
                new BigDecimal("9800.00"),
                6.0,
                0.2
        );

        assertThat(metrics.maxAdvanceSuggested()).isEqualByComparingTo("9215.00");
        assertThat(metrics.advanceRatio()).isEqualTo(0.98);
        assertThat(metrics.isAdvanceWithinLimit()).isFalse();
        assertThat(metrics.isViable()).isTrue();
    }

    @Test
    void calculateHandlesZeroFaceValueWithoutInvalidRatio() {
        FinancialMetrics metrics = service.calculate(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                1.0,
                0.5
        );

        assertThat(metrics.faceValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(metrics.advanceRatio()).isZero();
        assertThat(metrics.maxAdvanceSuggested()).isEqualByComparingTo("0.00");
        assertThat(metrics.isAdvanceWithinLimit()).isTrue();
        assertThat(metrics.isViable()).isFalse();
    }
}
