package com.cypher.analysis.engine.rules;

public record RuleResult(
        String ruleName,
        String category,
        double score,
        double weight,
        double contribution,
        String direction,
        String explanation,
        String dataSource
) {

    public static final String INCREASE = "INCREASE";
    public static final String DECREASE = "DECREASE";

    public static String direction(double score) {
        return direction(score, 0.0);
    }

    public static String direction(double score, double threshold) {
        return score > threshold ? INCREASE : DECREASE;
    }

    public static RuleResult of(
            String ruleName,
            String category,
            double score,
            double weight,
            String direction,
            String explanation,
            String dataSource
    ) {
        double clamped = clamp(score);
        return new RuleResult(
                ruleName,
                category,
                clamped,
                weight,
                clamped * weight,
                direction,
                explanation,
                dataSource
        );
    }

    public static RuleResult fallback(String ruleName, String category, double weight) {
        double conservativeScore = 0.4;
        return new RuleResult(
                ruleName,
                category,
                conservativeScore,
                weight,
                conservativeScore * weight,
                INCREASE,
                "Fonte de dados indisponível — score conservador aplicado automaticamente.",
                "FALLBACK"
        );
    }

    public boolean isFallback() {
        return "FALLBACK".equals(dataSource);
    }

    private static double clamp(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }
}