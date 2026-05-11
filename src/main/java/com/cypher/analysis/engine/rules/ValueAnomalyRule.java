package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import com.cypher.analysis.engine.*;


public class ValueAnomalyRule implements RiskRule {

    private static final double WEIGHT = 0.5;

    @Override
    public RuleResult evaluate(ScoringContext context) {
        double value = context.nfeData().getValorTotal().doubleValue();
        double avg = context.issuerAvgValue().doubleValue();

        if (avg > 0 && value > avg * 3){
            return RuleResult.of(getName(), "fruad_detection", 0.8, WEIGHT,
                    "INCREASE", "Valor da nota 3x acima da média do cedente.", "NFE_DATA");
        }
        return RuleResult.of(getName(), "fraud_detection", 0.0, WEIGHT, "DECREASE",
                "Valor dentro da normalidade", "NFE_DATA");
    }

    @Override
    public String getName() {
        return "ValueAnomalyRule";
    }

}