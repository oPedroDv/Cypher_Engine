package com.cypher.analysis.engine;

import com.cypher.analysis.engine.rules.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(RuleRegistry.class);
    private static final String MODEL_VERSION = "rule_engine_v1.0";
    private static final double WEIGHT_TOLERANCE = 0.001;

    private final List<RiskRule> activeRules;

    public RuleRegistry(
            SefazStatusRule      sefazStatusRule,
            IssuerHistoryRule    issuerHistoryRule,
            PayerHistoryRule     payerHistoryRule,
            DuplicateInvoiceRule duplicateInvoiceRule,
            CnpjStatusRule       cnpjStatusRule,
            MaturityRiskRule     maturityRiskRule,
            ValueAnomalyRule     valueAnomalyRule
    ) {
        this.activeRules = List.of(
                sefazStatusRule,
                issuerHistoryRule,
                payerHistoryRule,
                duplicateInvoiceRule,
                cnpjStatusRule,
                maturityRiskRule,
                valueAnomalyRule
        );

        validateWeights();
        log.info("RuleRegistry iniciado. Modelo: {}. Regras ativas: {}.", MODEL_VERSION, activeRules.size());
    }

    public List<RiskRule> getActiveRules() { return activeRules; }
    public String getModelVersion()        { return MODEL_VERSION; }

    private void validateWeights() {
        double totalWeight = activeRules.stream()
                .mapToDouble(RiskRule::getWeight)
                .sum();

        log.debug("Soma dos pesos das regras: {}", totalWeight);

        if (Math.abs(totalWeight - 1.0) > WEIGHT_TOLERANCE) {
            String details = activeRules.stream()
                    .map(r -> "%s=%.2f".formatted(r.getName(), r.getWeight()))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("nenhuma regra");

            throw new IllegalStateException(
                    ("Pesos das regras não somam 1.0. Soma atual: %.4f. Distribuição: [%s].")
                            .formatted(totalWeight, details)
            );
        }
    }
}