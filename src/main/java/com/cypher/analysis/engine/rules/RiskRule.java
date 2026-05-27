package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;

public interface RiskRule {

    RuleResult evaluate(ScoringContext context);
    String getName();
    double getWeight();
    default String getVersion() {
        return "1.0";
    }
}