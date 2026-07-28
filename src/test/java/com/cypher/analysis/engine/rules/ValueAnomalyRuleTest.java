package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import com.cypher.testutil.ScoringContextFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ValueAnomalyRuleTest {

    private final ValueAnomalyRule rule = new ValueAnomalyRule();

    @Test
    void exposesNameAndWeight() {
        assertThat(rule.getName()).isEqualTo("value_anomaly");
        assertThat(rule.getWeight()).isEqualTo(0.05);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-100.00"})
    void appliesLightUncertaintyWithoutUsableAverage(BigDecimal avgValue) {
        RuleResult result = rule.evaluate(context(new BigDecimal("10000.00"), avgValue));

        assertThat(result.score()).isEqualTo(0.30);
        assertThat(result.direction()).isEqualTo("INCREASE");
        assertThat(result.explanation()).contains("Sem histórico de valor médio");
        assertThat(result.dataSource()).isEqualTo("INTERNAL_HISTORY");
    }

    @ParameterizedTest
    @CsvSource({
            " 20000.00, 0.00, DECREASE",
            " 25000.00, 0.40, INCREASE",
            " 45000.00, 0.75, INCREASE",
            " 70000.00, 0.90, INCREASE",
            "100000.00, 1.00, INCREASE"
    })
    void scoresValueDeviationFromIssuerAverage(BigDecimal totalAmount,
                                               double expectedScore,
                                               String expectedDirection) {
        RuleResult result = rule.evaluate(context(totalAmount, new BigDecimal("10000.00")));

        assertThat(result.score()).isEqualTo(expectedScore);
        assertThat(result.direction()).isEqualTo(expectedDirection);
        assertThat(result.category()).isEqualTo("fraud_detection");
        assertThat(result.contribution()).isEqualTo(expectedScore * rule.getWeight());
        assertThat(result.dataSource()).isEqualTo("NFE_DATA");
    }

    @Test
    void reportsRatioForExtremeAnomaly() {
        RuleResult result = rule.evaluate(context(new BigDecimal("100000.00"), new BigDecimal("10000.00")));

        assertThat(result.explanation()).contains("anomalia extrema");
    }

    private ScoringContext context(BigDecimal totalAmount, BigDecimal issuerAvgValue) {
        return ScoringContextFixture.base()
                .nfeData(ScoringContextFixture.nfe(totalAmount, LocalDate.now().plusDays(30)))
                .issuerAvgValue(issuerAvgValue)
                .build();
    }
}
