package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.SefazStatus;
import com.cypher.testutil.ScoringContextFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SefazStatusRuleTest {

    private final SefazStatusRule rule = new SefazStatusRule();

    @Test
    void exposesNameWeightAndVersion() {
        assertThat(rule.getName()).isEqualTo("sefaz_status");
        assertThat(rule.getWeight()).isEqualTo(0.25);
        assertThat(rule.getVersion()).isEqualTo("1.0");
    }

    @ParameterizedTest
    @CsvSource({
            "AUTHORIZED,     0.00, DECREASE",
            "PENDING,        0.80, INCREASE",
            "CANCELLED,      1.00, INCREASE",
            "DENIED,         1.00, INCREASE",
            "UNAVAILABLE,    0.75, INCREASE",
            "ERROR,          0.75, INCREASE",
            "NOT_CONFIGURED, 0.35, INCREASE"
    })
    void scoresEachSefazStatus(SefazStatus status, double expectedScore, String expectedDirection) {
        RuleResult result = rule.evaluate(ScoringContextFixture.base().sefazStatus(status).build());

        assertThat(result.score()).isEqualTo(expectedScore);
        assertThat(result.direction()).isEqualTo(expectedDirection);
        assertThat(result.category()).isEqualTo("nfe_validation");
        assertThat(result.dataSource()).isEqualTo("SEFAZ");
        assertThat(result.contribution()).isEqualTo(expectedScore * rule.getWeight());
        assertThat(result.explanation()).isNotBlank();
        assertThat(result.isFallback()).isFalse();
    }

    @Test
    void treatsMissingStatusAsUnavailable() {
        RuleResult result = rule.evaluate(ScoringContextFixture.base().sefazStatus(null).build());

        assertThat(result.score()).isEqualTo(0.75);
        assertThat(result.explanation()).contains("SEFAZ indisponível");
    }
}
