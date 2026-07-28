package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import org.springframework.stereotype.Component;

@Component
public class IssuerHistoryRule implements RiskRule {

    private static final double WEIGHT = 0.20;

    @Override
    public RuleResult evaluate(ScoringContext context) {
        int total    = context.issuerTotalInvoices();
        int defaults = context.issuerDefaultCount();

        if (total == 0) {
            return RuleResult.of(getName(), "behavioral", 0.35, WEIGHT, "INCREASE",
                    "Cedente sem histórico no sistema — incerteza elevada.", "INTERNAL_HISTORY");
        }

        double defaultRate = (double) defaults / total;
        double score;
        String explanation;

        if (defaultRate == 0.0 && total >= 10) {
            score       = 0.0;
            explanation = "Cedente com %d operações e zero inadimplências — perfil excelente.".formatted(total);
        } else if (defaultRate == 0.0) {
            score       = 0.10;
            explanation = "Cedente com %d operação(ões) sem inadimplências — histórico inicial positivo.".formatted(total);
        } else if (defaultRate < 0.05) {
            score       = 0.25;
            explanation = "Taxa de inadimplência baixa: %.1f%% (%d/%d operações).".formatted(defaultRate * 100, defaults, total);
        } else if (defaultRate < 0.15) {
            score       = 0.55;
            explanation = "Taxa de inadimplência moderada: %.1f%% (%d/%d operações).".formatted(defaultRate * 100, defaults, total);
        } else {
            score       = 0.85;
            explanation = "Taxa de inadimplência alta: %.1f%% (%d/%d operações) — cedente de alto risco.".formatted(defaultRate * 100, defaults, total);
        }

        return RuleResult.of(getName(), "behavioral", score, WEIGHT, RuleResult.direction(score, 0.10), explanation, "INTERNAL_HISTORY");
    }

    @Override
    public String getName() { return "issuer_history"; }

    @Override
    public double getWeight() { return WEIGHT; }
}