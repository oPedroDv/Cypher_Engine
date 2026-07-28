package com.cypher.analysis.service;

import com.cypher.analysis.domain.FinancialMetrics;
import com.cypher.analysis.engine.RiskEngineConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FinancialMetricsService {

    private final RiskEngineConfig config;

    @Autowired
    public FinancialMetricsService(RiskEngineConfig config) {
        this.config = config;
    }

    public FinancialMetricsService() {
        this(new RiskEngineConfig());
    }

    public FinancialMetrics calculate(
            BigDecimal faceValue,
            BigDecimal requestedAdvance,
            double requestedMonthlyRate,
            double riskScore
    ) {
        BigDecimal advance = requestedAdvance != null
                ? requestedAdvance
                : faceValue.multiply(BigDecimal.valueOf(config.getDefaultAdvanceRatio()));
        return FinancialMetrics.calculate(faceValue, advance, requestedMonthlyRate, riskScore,
                BigDecimal.valueOf(config.getFinancialLossMultiplier()),
                BigDecimal.valueOf(config.getFinancialAdvanceHaircut()),
                BigDecimal.valueOf(config.getFinancialMaxAdvanceRatio()),
                BigDecimal.valueOf(config.getFinancialRateRiskPremium()));
    }
}
