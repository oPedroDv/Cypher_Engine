package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import org.springframework.stereotype.Component;

@Component
public class DuplicateInvoiceRule implements RiskRule {

    private static final double WEIGHT = 0.10;
    @Override
    public RuleResult evaluate(ScoringContext context) {
        int pairTotal = context.pairTotalInvoices();
        int pairDefaults = context.pairDefaultCount();

        if (pairTotal==0) {
            return RuleResult.of(
                    getName(), "fraud_detection", 0.25, WEIGHT, "INCREASE",
                    String.format("Par cedente-scado com %d operação(ões) anteriores sem inadimplências.", pairTotal),
                    "INTERNAL_HISTORY"
            );
        }

        double defaultRate = (double) pairDefaults / pairTotal;
        double score = Math.min(1.0, defaultRate * 2.0);
        return RuleResult.of(
                getName(), "fraud_detection", score, WEIGHT, "INCREASE",
                String.format("Par cedente-sacado com histórico de inadimplência: %d/%d operações (%.0f%%).",
                        pairDefaults, pairTotal, defaultRate * 100),
                "INTERNAL_HISTORY"
        );
    }

    @Override
    public String getName() {
        return "duplicate_invoice";
    }
}