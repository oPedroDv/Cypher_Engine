package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import com.cypher.analysis.engine.*;


public class ValueAnomalyRule implements RiskRule {

    @Override
    public RuleResult evaluate(ScoringContext context) {
        double value = context.getInvocie().getValue();
        double avg = context.getMetrics().getAvarageValue();

        if (value > avg * 3){
            return new RuleResult(getName(), 30, "Valor anormal.");
        }
        return new RuleResult(getName(), 0, "OK");
    }

    @Override
    public String getName() {
        return "ValueAnomalyRule";
    }

}