package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import org.springframework.stereotype.Component;

@Component
public class PayerHistoryRule implements RiskRule {

    private static final double WEIGHT = 0.20;

    @Override
    public RuleResult evaluate(ScoringContext context) {
        int total    = context.payerTotalInvoices();
        int defaults = context.payerDefaultCount();
        int late     = context.payerLatePaymentCount();

        if (total == 0) {
            return RuleResult.of(getName(), "behavioral", 0.30, WEIGHT, "INCREASE",
                    "Sacado sem histórico no sistema — comportamento de pagamento desconhecido.",
                    "INTERNAL_HISTORY");
        }

        double defaultRate = (double) defaults / total;
        double lateRate    = (double) late / total;
        double score;
        String explanation;

        if (defaultRate == 0.0 && lateRate == 0.0 && total >= 10) {
            score       = 0.0;
            explanation = "Sacado com %d pagamento(s) em dia, sem inadimplências — perfil excelente.".formatted(total);
        } else if (defaultRate == 0.0 && lateRate == 0.0) {
            score       = 0.10;
            explanation = "Sacado com %d pagamento(s) em dia, sem inadimplências — histórico inicial positivo.".formatted(total);
        } else if (defaultRate == 0.0 && lateRate < 0.10) {
            score       = 0.15;
            explanation = "Sacado sem inadimplências, com %.0f%% de atrasos — perfil aceitável.".formatted(lateRate * 100);
        } else if (defaultRate < 0.05) {
            score       = 0.35;
            explanation = "Sacado com %.1f%% de inadimplência e %.0f%% de atrasos.".formatted(defaultRate * 100, lateRate * 100);
        } else if (defaultRate < 0.15) {
            score       = 0.65;
            explanation = "Sacado com taxa de inadimplência moderada: %.1f%% (%d/%d).".formatted(defaultRate * 100, defaults, total);
        } else {
            score       = 0.90;
            explanation = "Sacado com alta inadimplência: %.1f%% (%d/%d) — risco de não pagamento elevado.".formatted(defaultRate * 100, defaults, total);
        }

        return RuleResult.of(getName(), "behavioral", score, WEIGHT, RuleResult.direction(score, 0.10), explanation, "INTERNAL_HISTORY");
    }

    @Override
    public String getName() { return "payer_history"; }

    @Override
    public double getWeight() { return WEIGHT; }
}
