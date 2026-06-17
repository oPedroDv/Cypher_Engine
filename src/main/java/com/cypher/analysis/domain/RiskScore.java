package com.cypher.analysis.domain;

public record RiskScore(double value) {

    private static final double LOW_THRESHOLD      = 0.20;
    private static final double MEDIUM_THRESHOLD   = 0.50;
    private static final double HIGH_THRESHOLD     = 0.75;

    public RiskScore {
        if (value < 0. || value > 1.0) {
            throw new IllegalArgumentException("Score deve estar entre 0.0 e 1.0. O Valor recebido foi: " + value);
        }
    }

    public static RiskScore of(double value) {

        double clamped = Math.min(1.0, Math.max(0.0, value));
        return new RiskScore(clamped);
    }

    public RiskLevel level() {
        if (value < LOW_THRESHOLD) return RiskLevel.LOW;
        if (value < MEDIUM_THRESHOLD) return RiskLevel.MEDIUM;
        if (value < HIGH_THRESHOLD) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }

    public boolean isLow() {return level() == RiskLevel.LOW;}
    public boolean isMedium() {return level() == RiskLevel.MEDIUM;}
    public boolean isHigh()     { return level() == RiskLevel.HIGH; }
    public boolean isCritical() { return level() == RiskLevel.CRITICAL; }

    public boolean requiresAttention() {
        return value >= LOW_THRESHOLD;
    }

    public boolean blockAnticipation() {
        return level() == RiskLevel.CRITICAL;
    }

    @Override
    public String toString() {
        return String.format("RiskScore{value=%.4f, level=%s}", value, level());
    }
}
