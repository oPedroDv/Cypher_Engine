package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import org.springframework.stereotype.Component;

@Component
public class SefazStatusRule implements RiskRule {
    
    private static final double WEIGHT = 0.30;

    @Override
    public RuleResult evaluate(ScoringContext context) {
        String status = context.sefazStatus();

        if ("UNAVAILABLE".equals(status)) {
            return RuleResult.fallback(getName(),"nfe_version", WEIGHT);
        }

        double score;
        String explanation;

        switch (status) {
            case "AUTHORIZED" -> {
                explanation = "NF-e autorizada e em situação regular no SEFAZ.";
            }
            case "PENDING" -> {
                explanation = "Status da NF-e não confirmado no SEFAZ - Operação de antecipação inválida.";
            }
            case "CANCELLED" -> {
                explanation = "NF-e cancelada no SEFAZ - operação de antecipação inválida.";
            }
            case "DENIED" -> {
                explanation = "NF-e denegada no SEFAZ - indica irregularidade fiscal grave";
            }
            default -> {
                explanation = "Status SEFAZ desconhecido: " + status;
            }
        }
        String direction = score > 0.0 ? "INCREASE" : "DECREASE";

        return RuleResult.of(getName(), "nfe_validation", score, WEIGHT, direction, explanation, "SEFAZ");
    }

    @Override
    public String getName() {
        return "sefaz_name";
    }
}