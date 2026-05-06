package com.cypher.analysis.engine;


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
    public static RuleResult of(
            String ruleName,
            String category,
            double score,
            double weight,
            String direction,
            String explanation,
            String dataSource
    ) {
        return new RuleResult(
                ruleName,
                category,
                clamp(score),
                weight,
                clamp(score) * weight,
                direction,
                explanation,
                dataSource
        );
    }

    public static RuleResult fallback(String ruleName, String category, double weight){
        double conservativeScore = 0.4;
        return new RuleResult(
                ruleName,
                category,
                conservativeScore,
                weight,
                conservativeScore * weight,
                "INCREASE",
                "Fonte de dados indisponível - score conservador aplicado automaticamente.",
                "FALLBACK"
        );
    }

    private static double clamp (double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }

    public boolean isFallBack() {
        return "FALLBACK".equals(dataSource);
    }
}