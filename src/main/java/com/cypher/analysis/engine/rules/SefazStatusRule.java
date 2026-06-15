package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import com.cypher.analysis.engine.SefazStatus;
import org.springframework.stereotype.Component;

@Component
public class SefazStatusRule implements RiskRule {

    private static final double WEIGHT = 0.25;

    @Override
    public RuleResult evaluate(ScoringContext context) {
        SefazStatus status = context.sefazStatus();

        double score;
        String explanation;

        switch (status) {
            case AUTHORIZED -> {
                score       = 0.0;
                explanation = "NF-e autorizada e em situação regular na SEFAZ.";
            }
            case PENDING -> {
                score       = 0.80;
                explanation = "Status da NF-e não confirmado na SEFAZ — antecipação sobre documento pendente é inválida.";
            }
            case CANCELLED -> {
                score       = 1.0;
                explanation = "NF-e cancelada na SEFAZ — operação de antecipação inválida.";
            }
            case DENIED -> {
                score       = 1.0;
                explanation = "NF-e denegada na SEFAZ — indica irregularidade fiscal grave.";
            }
            case UNAVAILABLE -> {
                score       = 0.75;
                explanation = "SEFAZ indisponível — não foi possível confirmar existência e autorização da NF-e.";
            }
            case ERROR -> {
                score       = 0.75;
                explanation = "Erro na consulta SEFAZ — documento não confirmado por fonte oficial.";
            }
            default -> {
                score       = 0.65;
                explanation = "Status SEFAZ desconhecido: " + status + " — score conservador aplicado.";
            }
        }

        String direction = score > 0.0 ? "INCREASE" : "DECREASE";
        return RuleResult.of(getName(), "nfe_validation", score, WEIGHT, direction, explanation, "SEFAZ");
    }

    @Override
    public String getName() { return "sefaz_status"; }

    @Override
    public double getWeight() { return WEIGHT; }
}
