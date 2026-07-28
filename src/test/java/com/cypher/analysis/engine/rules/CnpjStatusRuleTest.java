package com.cypher.analysis.engine.rules;

import com.cypher.company.domain.CnpjStatus;
import com.cypher.testutil.ScoringContextFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class CnpjStatusRuleTest {

    private final CnpjStatusRule rule = new CnpjStatusRule();

    @Test
    void exposesNameAndWeight() {
        assertThat(rule.getName()).isEqualTo("cnpj_status");
        assertThat(rule.getWeight()).isEqualTo(0.10);
    }

    @ParameterizedTest
    @CsvSource({
            "ACTIVE,    ACTIVE,    0.00, DECREASE",
            "SUSPENDED, ACTIVE,    0.60, INCREASE",
            "UNFIT,     ACTIVE,    0.85, INCREASE",
            "CLOSED,    ACTIVE,    1.00, INCREASE",
            "NULLIFIED, ACTIVE,    1.00, INCREASE",
            "UNKNOWN,   ACTIVE,    0.60, INCREASE",
            "ACTIVE,    SUSPENDED, 0.60, INCREASE",
            "ACTIVE,    CLOSED,    1.00, INCREASE"
    })
    void scoresWorstCnpjStatusOfBothParties(CnpjStatus issuerStatus, CnpjStatus payerStatus,
                                            double expectedScore, String expectedDirection) {
        RuleResult result = rule.evaluate(ScoringContextFixture.base()
                .issuerCnpjStatus(issuerStatus)
                .payerCnpjStatus(payerStatus)
                .build());

        assertThat(result.score()).isEqualTo(expectedScore);
        assertThat(result.direction()).isEqualTo(expectedDirection);
        assertThat(result.category()).isEqualTo("cnpj_validation");
        assertThat(result.dataSource()).isEqualTo("RECEITA_FEDERAL");
        assertThat(result.contribution()).isEqualTo(expectedScore * rule.getWeight());
    }

    @Test
    void explanationNamesTheIssuerWhenItIsTheRiskiestParty() {
        RuleResult result = rule.evaluate(ScoringContextFixture.base()
                .issuerCnpjStatus(CnpjStatus.CLOSED)
                .payerCnpjStatus(CnpjStatus.ACTIVE)
                .build());

        assertThat(result.explanation()).isEqualTo("CNPJ do cedente baixado — empresa encerrada. Antecipação inviável.");
    }

    @Test
    void explanationNamesThePayerWhenItIsTheRiskiestParty() {
        RuleResult result = rule.evaluate(ScoringContextFixture.base()
                .issuerCnpjStatus(CnpjStatus.ACTIVE)
                .payerCnpjStatus(CnpjStatus.NULLIFIED)
                .build());

        assertThat(result.explanation()).isEqualTo("CNPJ do sacado nulo ou cancelado — possível fraude.");
    }

    @Test
    void treatsMissingStatusesAsUnknown() {
        RuleResult result = rule.evaluate(ScoringContextFixture.base()
                .issuerCnpjStatus(null)
                .payerCnpjStatus(null)
                .build());

        assertThat(result.score()).isEqualTo(0.60);
        assertThat(result.explanation()).contains("não verificada");
    }
}
