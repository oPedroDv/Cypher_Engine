package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import com.cypher.analysis.engine.rules.RiskRule;
import org.springframework.stereotype.Component;

@Component
public class PayerHistoryRule implements RiskRule {

    private static final double WEIGHT = 0.15;

    @Override
    public RuleResult evaluate(ScoringContext context) {
        int total = context.payerTotalInvoices();
        int defaults = context.payerDefaultCount();
        int late = context.payerLatePaymentCount();

        if (total == 0) {
            return RuleResult.of(
                    getName(), "behavorial", 0.30, WEIGHT, "INCREASE",
                    "Sacado sem histórico no sistema - comportamento de pagamento desconhecido.", "INTERNAL_HISTORY"
            );
        }

        double defaultRate = (double) defaults / total;
        double lateRate = (double) late / total;
        double score;
        String explanation;

        if (defaultRate == 0.0 && lateRate == 0.0) {
            score = 0.0;
            explanation = String.format("Sacado com %d pagamento(s) em dia, sem inadimplências — perfil excelente.", total);
        } else if (defaultRate == 0.0 && lateRate < 0.10) {
            score = 0.15;
            explanation = String.format("Sacado sem inadimplências, com %.0f%% de atrasos — perfil aceitável.", lateRate * 100);
        } else if (defaultRate < 0.5) {
            score = 0.35;
            explanation =  String.format("Sacado com %.1f%% de inadimplência e %.0f%% de atrasos.", defaultRate * 100, lateRate * 100);
        } else if (defaultRate < 0.15) {
            score = 0.65;
            explanation = String.format("Sacado com taxa de inadimplência moderada: %.1f%% (%d/%d).",defaultRate * 100, defaults, total);
        } else {
            score = 0.90;
            explanation = String.format("Sacado com alta inadimplência: %.1f%% (%d/%d) — risco de não pagamento elevado.",defaultRate * 100, defaults, total);
        }

        return RuleResult.of(
                getName(), "behavorial", score, WEIGHT, score > 0.1 ? "INCREASE" : "DECREASE", explanation, "INTERNAL_HISTORY"
        );
    }

    @Override
    public String getName() {
        return "payer_history";
    }
}