package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;

public interface RiskRule {
    RuleResult evaluate(ScoringContext context);
    String getName();
    default String version(){
        return "1.0";
    }
}

