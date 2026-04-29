package com.cypher.analysis.domain;

public enum RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL;

    public static RiskLevel from (double score) {
        if (score < 0.30) return LOW;
        if (score < 0.60) return MEDIUM;
        if (score < 0.80) return HIGH;
        return CRITICAL;
    }
}
