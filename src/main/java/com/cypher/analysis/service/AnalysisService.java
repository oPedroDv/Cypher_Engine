package com.cypher.analysis.service;

import com.cypher.analysis.api.dto.AnalysisRequest;
import com.cypher.analysis.api.dto.AnalysisResponse;
import com.cypher.analysis.domain.*;
import com.cypher.analysis.engine.RiskEngineService;
import com.cypher.analysis.engine.ScoringContext;
import com.cypher.analysis.repository.InvoiceRepository;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.shared.exception.DuplicateInvoiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final InvoiceRepository invoiceRepository;
    private final RiskAnalysisRepository riskRepository;
    private final RiskEngineService riskEngine;
    private final FinancialMetricsService financialMetricsService;
    private final XmlStorageService xmlStorageService;
    private final IdempotencyService idempotencyService;

    public AnalysisService(
            InvoiceRepository invoiceRepository,
            RiskAnalysisRepository riskRepository,
            RiskEngineService riskEngine,
            FinancialMetricsService financialMetricsService,
            XmlStorageService xmlStorageService,
            IdempotencyService idempotencyService) {
        this.invoiceRepository = invoiceRepository;
        this.riskRepository = riskRepository;
        this.riskEngine = riskEngine;
        this.financialMetricsService = financialMetricsService;
        this.xmlStorageService = xmlStorageService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public AnalysisResponse analyze(AnalysisRequest request) {
        String existingAnalysisId = idempotencyService.checkOrReverse(request.idempotencyKey());
        if (existingAnalysisId != null) {
            return findById(UUID.fromString(existingAnalysisId))
                    .orElseThrow(() -> new IllegalStateException("Erro ao recuperar análise idempotente"));
        }

        try {
            String xmlPath = xmlStorageService.store(request.xmlBase64());

            Invoice invoice = Invoice.of(request.xmlBase64());
            validateDuplicity(invoice.getChaveNfe());

            Invoice savedInvoice = invoiceRepository.save(invoice);

            NFeData nfeData = NFeParser.parse(request.xmlBase64());

            ScoringContext context = buildScoringContext(nfeData, request);

            RiskEngineService.EngineResult engineResult = riskEngine.score(context);

            FinancialMetrics metrics = financialMetricsService.calculate(
                    nfeData.getValorTotal(),
                    request.requestedAdvanceValue(),
                    request.requestedMonthlyRate(),
                    engineResult.score()
            );

            RiskAnalysis analysis = RiskAnalysis.of(
                    savedInvoice,
                    engineResult.score(),
                    engineResult.modelVersion()
            );

            RiskAnalysis savedAnalysis = riskRepository.save(analysis);

            return AnalysisResponse.from(savedAnalysis, false);

        } catch (Exception e) {
            idempotencyService.release(request.idempotencyKey());
            log.error("Falha na análise de risco: {}", e.getMessage());
            throw e;
        }
    }

    public java.util.Optional<AnalysisResponse> findById(UUID id) {
        return riskRepository.findById(id)
                .map(analysis -> AnalysisResponse.from(analysis, true));
    }

    private void validateDuplicity(String chaveNfe) {
        invoiceRepository.findByChaveNfe(chaveNfe).ifPresent(invoice -> {
            riskRepository.findTopByInvoiceIdOrderByCreatedAtDesc(invoice.getId())
                    .ifPresent(analysis -> {
                        throw new DuplicateInvoiceException(chaveNfe, analysis.getId().toString());
                    });
        });
    }

    private ScoringContext buildScoringContext(NFeData nfeData, AnalysisRequest request) {
        return ScoringContext.builder()
                .nfeData(nfeData)
                .sefazStatus(nfeData.getStatus())
                .issuerCnpjStatus(CnpjStatus.ACTIVE)
                .payerCnpjStatus(CnpjStatus.ACTIVE)
                .requestedAdvanceValue(request.requestedAdvanceValue())
                .requestedMonthlyRate(request.requestedMonthlyRate())
                .hasUnavailableSource(false)
                .build();
    }
}