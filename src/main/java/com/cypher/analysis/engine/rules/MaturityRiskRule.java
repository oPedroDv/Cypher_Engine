package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import org.apache.tomcat.util.digester.Rule;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class MaturityRiskRule implements RiskRule{

    private static final double WEIGHT = 0.10;

    @Override
    public RuleResult evaluate(ScoringContext context) {
        LocalDate dueDate = context.nfeData().getDataVencimento();

        if (dueDate == null) {
            return RuleResult.of(
                    getName(), "liquidity_risk", 0.4, WEIGHT, "INCREASE", "Data de vencimento não identificada na NF-e.", "NFE_DATA"
            );
        }

        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);

        double score;
        String explanation;

        if (daysUntilDue < 0) {
            score = 1.0;
            explanation = String.format("NF-e vencida há %d dia(s) — antecipação inviável.", Math.abs(daysUntilDue));
        } else if (daysUntilDue <= 3) {
            score = 0.85;
            explanation = String.format("Vencimento em %d dia(s) — margem de cobrança insuficiente.\", daysUntilDue");
        } else if (daysUntilDue <= 7) {
            score = 0.60;
            explanation = String.format("Vencimento em %d dias — prazo curto para cobrança.\", daysUntilDue");
        } else if (daysUntilDue <= 15) {
            score = 0.30;
            explanation = String.format("Vencimento em %d dias — prazo aceitável com atenção.", daysUntilDue);
        } else if (daysUntilDue <= 90) {
            score = 0.0;
            explanation = String.format("Vencimento em %d dias — prazo ideal para antecipação.", daysUntilDue);
        } else {
            score = 0.15;
            explanation =  String.format("Vencimento em %d dias — exposição longa ao risco de crédito.", daysUntilDue);
        }

        return RuleResult.of(
                getName(), "liquidity_risk", score, WEIGHT, score > 0.0 ? "INCREASE" : "DECREASE", explanation, "NFE_DATA"
        );
    }

    @Override
    public String getName() {
        return "";
    }
}