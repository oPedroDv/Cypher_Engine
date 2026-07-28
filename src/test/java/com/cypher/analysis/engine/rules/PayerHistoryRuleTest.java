package com.cypher.analysis.engine.rules;

import com.cypher.testutil.ScoringContextFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PayerHistoryRuleTest {

    private final PayerHistoryRule rule = new PayerHistoryRule();

    @Test
    void exposesNameAndWeight() {
        assertThat(rule.getName()).isEqualTo("payer_history");
        assertThat(rule.getWeight()).isEqualTo(0.20);
    }

    @Test
    void penalizesPayerWithoutHistory() {
        RuleResult result = rule.evaluate(ScoringContextFixture.base()
                .payerTotalInvoices(0)
                .build());

        assertThat(result.score()).isEqualTo(0.30);
        assertThat(result.direction()).isEqualTo("INCREASE");
        assertThat(result.explanation()).contains("Sacado sem histórico");
    }

    @ParameterizedTest
    @CsvSource({
            "10, 0, 0, 0.00, DECREASE",
            " 4, 0, 0, 0.10, DECREASE",
            "20, 0, 1, 0.15, INCREASE",
            "10, 0, 5, 0.35, INCREASE",
            "50, 1, 0, 0.35, INCREASE",
            "20, 2, 0, 0.65, INCREASE",
            "10, 3, 0, 0.90, INCREASE"
    })
    void scoresPayerBehaviourBands(int total, int defaults, int late,
                                   double expectedScore, String expectedDirection) {
        RuleResult result = rule.evaluate(ScoringContextFixture.base()
                .payerTotalInvoices(total)
                .payerDefaultCount(defaults)
                .payerLatePaymentCount(late)
                .build());

        assertThat(result.score()).isEqualTo(expectedScore);
        assertThat(result.direction()).isEqualTo(expectedDirection);
        assertThat(result.category()).isEqualTo("behavioral");
        assertThat(result.dataSource()).isEqualTo("INTERNAL_HISTORY");
        assertThat(result.contribution()).isEqualTo(expectedScore * rule.getWeight());
    }

    @Test
    void reportsLatePaymentRateWhenNoDefaults() {
        RuleResult result = rule.evaluate(ScoringContextFixture.base()
                .payerTotalInvoices(20)
                .payerLatePaymentCount(1)
                .build());

        assertThat(result.explanation()).contains("5% de atrasos");
    }
}
