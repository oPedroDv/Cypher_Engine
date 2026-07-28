package com.cypher.analysis.engine.rules;

import com.cypher.testutil.ScoringContextFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MaturityRiskRuleTest {

    private final MaturityRiskRule rule = new MaturityRiskRule();

    @Test
    void exposesNameAndWeight() {
        assertThat(rule.getName()).isEqualTo("maturity_risk");
        assertThat(rule.getWeight()).isEqualTo(0.05);
    }

    @Test
    void penalizesInvoiceWithoutDueDate() {
        RuleResult result = rule.evaluate(contextForDueDate(null));

        assertThat(result.score()).isEqualTo(0.60);
        assertThat(result.direction()).isEqualTo("INCREASE");
        assertThat(result.explanation()).contains("Data de vencimento não identificada");
        assertThat(result.category()).isEqualTo("liquidity_risk");
    }

    @ParameterizedTest
    @CsvSource({
            " -5, 1.00, INCREASE",
            "  2, 0.95, INCREASE",
            "  6, 0.75, INCREASE",
            " 12, 0.45, INCREASE",
            " 30, 0.00, DECREASE",
            "120, 0.30, INCREASE"
    })
    void scoresMaturityWindows(int daysUntilDue, double expectedScore, String expectedDirection) {
        RuleResult result = rule.evaluate(contextForDueDate(LocalDate.now().plusDays(daysUntilDue)));

        assertThat(result.score()).isEqualTo(expectedScore);
        assertThat(result.direction()).isEqualTo(expectedDirection);
        assertThat(result.dataSource()).isEqualTo("NFE_DATA");
        assertThat(result.contribution()).isEqualTo(expectedScore * rule.getWeight());
    }

    @Test
    void reportsHowManyDaysOverdue() {
        RuleResult result = rule.evaluate(contextForDueDate(LocalDate.now().minusDays(3)));

        assertThat(result.explanation()).contains("vencida há 3 dia(s)");
    }

    private com.cypher.analysis.engine.ScoringContext contextForDueDate(LocalDate dueDate) {
        return ScoringContextFixture.base()
                .nfeData(ScoringContextFixture.nfe(new BigDecimal("10000.00"), dueDate))
                .build();
    }
}
