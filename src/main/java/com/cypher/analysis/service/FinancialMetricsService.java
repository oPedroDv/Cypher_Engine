package com.cypher.analysis.service;

import com.cypher.analysis.domain.FinancialMetrics;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FinancialMetricsService {

    public FinancialMetrics calculate(
            BigDecimal faceValue,
            BigDecimal requestedAdvance,
            double requestedMonthlyRate,
            double riskScore
    ) {
        BigDecimal advance = requestedAdvance != null ? requestedAdvance : faceValue.multiply(BigDecimal.valueOf(0.90));
        return FinancialMetrics.calculate(faceValue, advance, requestedMonthlyRate, riskScore);
    }
}