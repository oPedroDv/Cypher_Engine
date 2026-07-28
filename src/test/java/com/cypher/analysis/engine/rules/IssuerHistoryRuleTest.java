package com.cypher.analysis.engine.rules;

import com.cypher.testutil.ScoringContextFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class IssuerHistoryRuleTest {

    private final IssuerHistoryRule rule = new IssuerHistoryRule();

    @Test
    void exposesNameAndWeight() {
        assertThat(rule.getName()).isEqualTo("issuer_history");
        assertThat(rule.getWeight()).isEqualTo(0.20);
    }

    @Test
    void penalizesIssuerWithoutHistory() {
        RuleResult result = rule.evaluate(ScoringContextFixture.base()
                .issuerTotalInvoices(0)
                .issuerDefaultCount(0)
                .build());

        assertThat(result.score()).isEqualTo(0.35);
        assertThat(result.direction()).isEqualTo("INCREASE");
        assertThat(result.explanation()).contains("sem histórico");
        assertThat(result.dataSource()).isEqualTo("INTERNAL_HISTORY");
    }

    @ParameterizedTest
    @CsvSource({
            "10,  0, 0.00, DECREASE",
            " 5,  0, 0.10, DECREASE",
            "50,  1, 0.25, INCREASE",
            "20,  2, 0.55, INCREASE",
            "10,  3, 0.85, INCREASE"
    })
    void scoresIssuerDefaultRateBands(int total, int defaults, double expectedScore, String expectedDirection) {
        RuleResult result = rule.evaluate(ScoringContextFixture.base()
                .issuerTotalInvoices(total)
                .issuerDefaultCount(defaults)
                .build());

        assertThat(result.score()).isEqualTo(expectedScore);
        assertThat(result.direction()).isEqualTo(expectedDirection);
        assertThat(result.category()).isEqualTo("behavioral");
        assertThat(result.contribution()).isEqualTo(expectedScore * rule.getWeight());
    }

    @Test
    void reportsOperationCountForSpotlessIssuer() {
        RuleResult result = rule.evaluate(ScoringContextFixture.base()
                .issuerTotalInvoices(12)
                .issuerDefaultCount(0)
                .build());

        assertThat(result.explanation()).contains("12 operações").contains("perfil excelente");
    }
}
