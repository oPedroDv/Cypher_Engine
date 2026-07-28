package com.cypher.analysis.engine;

import com.cypher.analysis.engine.rules.CnpjStatusRule;
import com.cypher.analysis.engine.rules.DuplicateInvoiceRule;
import com.cypher.analysis.engine.rules.IssuerHistoryRule;
import com.cypher.analysis.engine.rules.MaturityRiskRule;
import com.cypher.analysis.engine.rules.PayerHistoryRule;
import com.cypher.analysis.engine.rules.RiskRule;
import com.cypher.analysis.engine.rules.RuleResult;
import com.cypher.analysis.engine.rules.SefazStatusRule;
import com.cypher.analysis.engine.rules.ValueAnomalyRule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class RuleRegistryTest {

    private RuleRegistry newRegistry(SefazStatusRule sefazStatusRule) {
        return new RuleRegistry(
                sefazStatusRule,
                new IssuerHistoryRule(),
                new PayerHistoryRule(),
                new DuplicateInvoiceRule(),
                new CnpjStatusRule(),
                new MaturityRiskRule(),
                new ValueAnomalyRule()
        );
    }

    @Test
    void registersAllRulesWithWeightsSummingToOne() {
        RuleRegistry registry = newRegistry(new SefazStatusRule());

        assertThat(registry.getActiveRules()).hasSize(7);
        assertThat(registry.getActiveRules())
                .extracting(RiskRule::getName)
                .containsExactly(
                        "sefaz_status",
                        "issuer_history",
                        "payer_history",
                        "duplicate_invoice",
                        "cnpj_status",
                        "maturity_risk",
                        "value_anomaly"
                );
        assertThat(registry.getActiveRules().stream().mapToDouble(RiskRule::getWeight).sum())
                .isCloseTo(1.0, within(0.001));
        assertThat(registry.getModelVersion()).isEqualTo("rule_engine_v1.0");
    }

    @Test
    void rejectsRuleSetWhoseWeightsDoNotSumToOne() {
        assertThatThrownBy(() -> newRegistry(new SefazStatusRule() {
            @Override
            public double getWeight() {
                return 0.50;
            }
        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Pesos das regras não somam 1.0")
                .hasMessageContaining("sefaz_status=");
    }

    @Test
    void activeRulesCollectionIsImmutable() {
        RuleRegistry registry = newRegistry(new SefazStatusRule());

        assertThatThrownBy(() -> registry.getActiveRules().add(new RiskRule() {
            @Override
            public RuleResult evaluate(ScoringContext context) {
                return RuleResult.fallback("extra", "test", 0.0);
            }

            @Override
            public String getName() {
                return "extra";
            }

            @Override
            public double getWeight() {
                return 0.0;
            }
        }))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
