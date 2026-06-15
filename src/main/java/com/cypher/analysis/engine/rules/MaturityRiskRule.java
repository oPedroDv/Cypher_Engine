package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class MaturityRiskRule implements RiskRule {

    private static final double WEIGHT = 0.05;

    @Override
    public RuleResult evaluate(ScoringContext context) {
        LocalDate dueDate = context.nfeData().getDueDate();

        if (dueDate == null) {
            return RuleResult.of(getName(), "liquidity_risk", 0.60, WEIGHT, "INCREASE",
                    "Data de vencimento não identificada na NF-e.", "NFE_DATA");
        }

        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
        double score;
        String explanation;

        if (daysUntilDue < 0) {
            score       = 1.0;
            explanation = "NF-e vencida há %d dia(s) — antecipação inviável.".formatted(Math.abs(daysUntilDue));
        } else if (daysUntilDue <= 3) {
            score       = 0.95;
            explanation = "Vencimento em %d dia(s) — margem de cobrança insuficiente.".formatted(daysUntilDue);
        } else if (daysUntilDue <= 7) {
            score       = 0.75;
            explanation = "Vencimento em %d dias — prazo curto para cobrança.".formatted(daysUntilDue);
        } else if (daysUntilDue <= 15) {
            score       = 0.45;
            explanation = "Vencimento em %d dias — prazo aceitável com atenção.".formatted(daysUntilDue);
        } else if (daysUntilDue <= 90) {
            score       = 0.0;
            explanation = "Vencimento em %d dias — prazo ideal para antecipação.".formatted(daysUntilDue);
        } else {
            score       = 0.30;
            explanation = "Vencimento em %d dias — exposição longa ao risco de crédito.".formatted(daysUntilDue);
        }

        String direction = score > 0.0 ? "INCREASE" : "DECREASE";
        return RuleResult.of(getName(), "liquidity_risk", score, WEIGHT, direction, explanation, "NFE_DATA");
    }

    @Override
    public String getName() { return "maturity_risk"; }

    @Override
    public double getWeight() { return WEIGHT; }
}
