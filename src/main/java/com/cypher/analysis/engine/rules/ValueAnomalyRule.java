package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ValueAnomalyRule implements RiskRule {

    private static final double WEIGHT = 0.05;

    @Override
    public RuleResult evaluate(ScoringContext context) {
        BigDecimal avgValue = context.issuerAvgValue();

        if (avgValue == null || avgValue.compareTo(BigDecimal.ZERO) <= 0) {
            return RuleResult.of(getName(), "fraud_detection", 0.20, WEIGHT, "INCREASE",
                    "Sem histórico de valor médio para o cedente — incerteza leve aplicada.",
                    "INTERNAL_HISTORY");
        }

        double value = context.nfeData().getTotalAmount().doubleValue();
        double avg   = avgValue.doubleValue();
        double ratio = value / avg;

        double score;
        String explanation;

        if (ratio <= 2.0) {
            score       = 0.0;
            explanation = "Valor da NF-e (%.2fx a média) dentro da normalidade histórica do cedente.".formatted(ratio);
        } else if (ratio <= 3.0) {
            score       = 0.40;
            explanation = "Valor da NF-e %.2fx acima da média do cedente — requer atenção.".formatted(ratio);
        } else if (ratio <= 5.0) {
            score       = 0.70;
            explanation = "Valor da NF-e %.2fx acima da média do cedente — anomalia relevante.".formatted(ratio);
        } else {
            score       = 0.90;
            explanation = "Valor da NF-e %.2fx acima da média do cedente — possível superfaturamento ou fraude.".formatted(ratio);
        }

        String direction = score > 0.0 ? "INCREASE" : "DECREASE";
        return RuleResult.of(getName(), "fraud_detection", score, WEIGHT, direction, explanation, "NFE_DATA");
    }

    @Override
    public String getName() { return "value_anomaly"; }

    @Override
    public double getWeight() { return WEIGHT; }
}
