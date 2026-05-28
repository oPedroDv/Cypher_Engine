package com.cypher.analysis.service;

import com.cypher.analysis.api.dto.AnalysisRequest;
import com.cypher.analysis.api.dto.AnalysisResponse;
import com.cypher.analysis.application.AnalyzeInvoiceUseCase;
import com.cypher.analysis.application.port.RiskAnalysisPersistencePort;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class AnalysisService {

    private final AnalyzeInvoiceUseCase analyzeInvoiceUseCase;
    private final RiskAnalysisPersistencePort riskAnalysisPersistence;

    public AnalysisService(AnalyzeInvoiceUseCase analyzeInvoiceUseCase,
                           RiskAnalysisPersistencePort riskAnalysisPersistence) {
        this.analyzeInvoiceUseCase = analyzeInvoiceUseCase;
        this.riskAnalysisPersistence = riskAnalysisPersistence;
    }

    public AnalysisResponse analyze(AnalysisRequest request) {
        return analyzeInvoiceUseCase.execute(request);
    }

    public Optional<AnalysisResponse> findById(UUID id) {
        return riskAnalysisPersistence.findById(id)
                .map(analysis -> AnalysisResponse.from(analysis, true));
    }
}
