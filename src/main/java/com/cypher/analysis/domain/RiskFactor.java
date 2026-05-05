package com.cypher.analysis.domain;

import com.cypher.analysis.engine.rules.RuleResult;

public record RiskFactor(
        String name,
        String category,
        double score,
        double weight,
        double contribution,
        String direction,
        String explanation,
        String dataSource,
        boolean isFallback
) {
    public static RiskFactor from(RuleResult result) {
        return new RiskFactor(
                result.ruleName(),
                result.category(),
                result.score(),
                result.weight(),
                result.contribution(),
                result.direction(),
                result.explanation(),
                result.dataSource(),
                result.isFallback()
        );
    }
    public boolean increasesRisk() {
        return "INCREASE".equals(direction);
    }
}