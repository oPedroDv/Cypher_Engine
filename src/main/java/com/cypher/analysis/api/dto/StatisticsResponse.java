package com.cypher.analysis.api.dto;

import java.util.List;
import java.util.Map;

public record StatisticsResponse(
        long totalAnalyses,
        long totalRisksFound,
        double avgScore,
        Map<String, Long> byRiskLevel,
        List<DailyCount> analysesLast30Days
) {
    public record DailyCount(String date, long count) {}
}
