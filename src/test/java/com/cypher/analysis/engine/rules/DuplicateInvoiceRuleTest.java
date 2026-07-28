package com.cypher.analysis.engine.rules;

import com.cypher.testutil.ScoringContextFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateInvoiceRuleTest {

    private final DuplicateInvoiceRule rule = new DuplicateInvoiceRule();

    @Test
    void exposesNameAndWeight() {
        assertThat(rule.getName()).isEqualTo("duplicate_invoice");
        assertThat(rule.getWeight()).isEqualTo(0.15);
    }

    @Test
    void flagsFirstOperationBetweenParties() {
        RuleResult result = rule.evaluate(ScoringContextFixture.base()
                .pairTotalInvoices(0)
                .pairDefaultCount(0)
                .build());

        assertThat(result.score()).isEqualTo(0.20);
        assertThat(result.direction()).isEqualTo("INCREASE");
        assertThat(result.explanation()).contains("primeira operação");
        assertThat(result.category()).isEqualTo("fraud_detection");
    }

    @ParameterizedTest
    @CsvSource({
            "10, 0, 0.0, DECREASE",
            "10, 1, 0.2, INCREASE",
            "10, 2, 0.4, INCREASE",
            "10, 6, 1.0, INCREASE",
            " 2, 2, 1.0, INCREASE"
    })
    void doublesPairDefaultRateAndCapsAtOne(int total, int defaults,
                                            double expectedScore, String expectedDirection) {
        RuleResult result = rule.evaluate(ScoringContextFixture.base()
                .pairTotalInvoices(total)
                .pairDefaultCount(defaults)
                .build());

        assertThat(result.score()).isEqualTo(expectedScore);
        assertThat(result.direction()).isEqualTo(expectedDirection);
        assertThat(result.contribution()).isEqualTo(expectedScore * rule.getWeight());
    }

    @Test
    void describesCleanAndDelinquentPairHistoryDifferently() {
        RuleResult clean = rule.evaluate(ScoringContextFixture.base()
                .pairTotalInvoices(4)
                .pairDefaultCount(0)
                .build());
        RuleResult delinquent = rule.evaluate(ScoringContextFixture.base()
                .pairTotalInvoices(4)
                .pairDefaultCount(1)
                .build());

        assertThat(clean.explanation()).contains("histórico positivo");
        assertThat(delinquent.explanation()).contains("inadimplência: 1/4");
    }
}
