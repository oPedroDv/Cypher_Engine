package com.cypher.analysis.engine;

import com.cypher.analysis.engine.rules.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleRegistry {

    private static final String MODEL_VERSION = "rule_engine_v1.0";

    private final List<RiskRule> activeRules;

    public RuleRegistry(
            SefazStatusRule sefazStatusRule,
            CnpjStatusRule cnpjStatusRule,
            DuplicateInvoiceRule duplicateInvoiceRule,
            IssuerHistoryRule issuerHistoryRule,
            PayerHistoryRule payerHistoryRule,
            MaturityRiskRule maturityRiskRule,
            ValueAnomalyRule valueAnomalyRule
    ) {
        this.activeRules = List.of(
                sefazStatusRule,
                cnpjStatusRule,
                duplicateInvoiceRule,
                issuerHistoryRule,
                payerHistoryRule,
                maturityRiskRule,
                valueAnomalyRule
        );

        validateWeights();
    }

    public List<RiskRule> getActiveRules() {
        return activeRules;
    }

    public String getModelVersion() {
        return MODEL_VERSION;
    }

    private void validateWeights() {
        // Cria um contexto dummy só para ler os pesos
        double totalWeight = activeRules.stream()
                .mapToDouble(rule -> rule.evaluate(dummyContext()).weight())
                .sum();

        if (Math.abs(totalWeight - 1.0) > 0.01) {
            throw new IllegalStateException(
                    "Pesos das regras não somam 1.0. Soma atual: " + totalWeight +
                            ". Ajuste os pesos em RuleRegistry."
            );
        }
    }

    private ScoringContext dummyContext() {
        var nfe = new com.cypher.analysis.domain.NFeData();
        return ScoringContext.builder().nfeData(nfe).build();
    }
}