package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;

public class CnpjStatusRule implements RiskRule {

    @Override
    public RuleResult evaluate(ScoringContext context) {
        boolean active = context.getMetrics().isCnpjActive();

        if (!active) {
            return new RuleResult(getName(), 50, "CNPJ inativo.");
        }
        return new RuleResult(getName(), 0, "OK");
    }

    @Override
    public String getName() {
        return "CnpjStatusRule";
    }
}