package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import org.springframework.stereotype.Component;

@Component
public class DuplicateInvoiceRule implements RiskRule {

    private static final double WEIGHT = 0.15;

    @Override
    public RuleResult evaluate(ScoringContext context) {
        int pairTotal    = context.pairTotalInvoices();
        int pairDefaults = context.pairDefaultCount();

        if (pairTotal == 0) {
            return RuleResult.of(getName(), "fraud_detection", 0.20, WEIGHT, "INCREASE",
                    "Par cedente-sacado sem histórico no sistema — primeira operação entre as partes.",
                    "INTERNAL_HISTORY");
        }

        double defaultRate = (double) pairDefaults / pairTotal;
        double score       = Math.min(1.0, defaultRate * 2.0);
        String explanation = pairDefaults == 0
                ? "Par cedente-sacado com %d operação(ões) sem inadimplências — histórico positivo.".formatted(pairTotal)
                : "Par cedente-sacado com inadimplência: %d/%d operações (%.0f%%).".formatted(pairDefaults, pairTotal, defaultRate * 100);

        String direction = score > 0.0 ? "INCREASE" : "DECREASE";
        return RuleResult.of(getName(), "fraud_detection", score, WEIGHT, direction, explanation, "INTERNAL_HISTORY");
    }

    @Override
    public String getName() { return "duplicate_invoice"; }

    @Override
    public double getWeight() { return WEIGHT; }
}