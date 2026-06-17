package com.cypher.analysis.domain;

public enum RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL;

    public static RiskLevel from(double score) {
        if (score < 0.20) return LOW;
        if (score < 0.50) return MEDIUM;
        if (score < 0.75) return HIGH;
        return CRITICAL;
    }
}
