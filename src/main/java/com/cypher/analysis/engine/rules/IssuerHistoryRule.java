package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import org.springframework.stereotype.Component;

@Component
public class IssuerHistoryRule implements RiskRule {

    public static final double WEIGHT = 0.20;

    @Override
    public RuleResult evaluate(ScoringContext context) {
        int total = context.issuerTotalInvoices();
        int defaults = context.issuerDefaultCount();
        if (total == 0){
            return RuleResult.of(getName(), "behavioral", 0.35, "WEIGHT", "INCREASE", "Cedente sem histórico no sistema - incerteza elevada.","INTERNAL_HISTORY");
        }

        double defaultRate = (double) defaults / total;
        double score = 0;
        String explanation;

        if (defaultRate == 0.0 && total >= 10) {
            explanation = String.format("Cedente com %d operações anteriores e zero inadimplências — perfil excelente.", total);
        } else if (defaultRate == 0.0) {
            score = 0.1;
            explanation = String.format("Cedente com %d operação(ões) sem inadimplências — histórico inicial positivo.", total);
        } else if (defaultRate < 0.05) {
            score = 0.25;
            explanation = String.format( "Taxa de inadimplência baixa: %.1f%% (%d/%d operações).", defaultRate * 100, defaults, total);
        } else if (defaultRate < 0.15) {
            score = 0.55;
            explanation = String.format("Taxa de inadimplência moderada: %.1f%% (%d/%d operações).", defaultRate * 100, defaults, total);
        } else {
            score = 0.85;
            explanation = String.format("Taxa de inadimplência alta: %.1f%% (%d/%d operações) — cedente de alto risco.", defaultRate * 100, defaults, total);
        }
        return RuleResult.of(
                getName(), "behavioral", score, WEIGHT,
                score > 0.1 ? "INCREASE" : "DECREASE",
                explanation, "INTERNAL_HISTORY"
        );
    }
    @Override
    public String getName() {
        return "issuer_history";
    }
}