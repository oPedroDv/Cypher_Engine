package com.cypher.analysis.application;

import com.cypher.analysis.api.dto.AnalysisRequest;
import com.cypher.analysis.api.dto.AnalysisResponse;
import com.cypher.analysis.application.port.IdempotencyPort;
import com.cypher.analysis.application.port.InvoicePersistencePort;
import com.cypher.analysis.application.port.RiskAnalysisPersistencePort;
import com.cypher.analysis.application.port.XmlStoragePort;
import com.cypher.analysis.domain.FinancialMetrics;
import com.cypher.analysis.domain.Invoice;
import com.cypher.analysis.domain.NFeData;
import com.cypher.analysis.domain.NFeParser;
import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.domain.RiskFactor;
import com.cypher.analysis.engine.RiskEngineService;
import com.cypher.analysis.engine.ScoringContext;
import com.cypher.analysis.engine.SefazStatus;
import com.cypher.analysis.service.FinancialMetricsService;
import com.cypher.company.domain.CnpjStatus;
import com.cypher.shared.exception.DuplicateInvoiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AnalyzeInvoiceUseCase {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeInvoiceUseCase.class);

    private final InvoicePersistencePort invoicePersistence;
    private final RiskAnalysisPersistencePort riskAnalysisPersistence;
    private final RiskEngineService riskEngine;
    private final FinancialMetricsService financialMetricsService;
    private final XmlStoragePort xmlStorage;
    private final IdempotencyPort idempotency;

    public AnalyzeInvoiceUseCase(
            InvoicePersistencePort invoicePersistence,
            RiskAnalysisPersistencePort riskAnalysisPersistence,
            RiskEngineService riskEngine,
            FinancialMetricsService financialMetricsService,
            XmlStoragePort xmlStorage,
            IdempotencyPort idempotency) {
        this.invoicePersistence = invoicePersistence;
        this.riskAnalysisPersistence = riskAnalysisPersistence;
        this.riskEngine = riskEngine;
        this.financialMetricsService = financialMetricsService;
        this.xmlStorage = xmlStorage;
        this.idempotency = idempotency;
    }

    @Transactional
    public AnalysisResponse execute(AnalysisRequest request) {
        String existingAnalysisId = idempotency.checkOrReverse(request.idempotencyKey());
        if (existingAnalysisId != null) {
            return riskAnalysisPersistence.findById(UUID.fromString(existingAnalysisId))
                    .map(analysis -> AnalysisResponse.from(analysis, true))
                    .orElseThrow(() -> new IllegalStateException("Erro ao recuperar análise idempotente: " + existingAnalysisId));
        }

        try {
            NFeData nfeData = NFeParser.parse(request.xmlBase64());

            validateDuplicity(nfeData.getAccessKey());

            Invoice invoice = Invoice.of(request.xmlBase64(), nfeData.getAccessKey());
            Invoice savedInvoice = invoicePersistence.save(invoice);

            xmlStorage.store(request.xmlBase64(), savedInvoice.getId(), nfeData.getAccessKey());

            ScoringContext context = buildScoringContext(nfeData, request);
            RiskEngineService.EngineResult engineResult = riskEngine.score(context);

            FinancialMetrics metrics = financialMetricsService.calculate(
                    nfeData.getTotalAmount(),
                    request.requestedAdvanceValue(),
                    request.requestedMonthlyRate(),
                    engineResult.score()
            );

            List<RiskFactor> factors = engineResult.factors().stream()
                    .map(RiskFactor::from)
                    .toList();

            RiskAnalysis analysis = RiskAnalysis.of(
                    savedInvoice,
                    engineResult.score(),
                    engineResult.modelVersion(),
                    factors,
                    metrics,
                    engineResult.dataPartial()
            );

            RiskAnalysis savedAnalysis = riskAnalysisPersistence.save(analysis);

            idempotency.confirm(request.idempotencyKey(), savedAnalysis.getId().toString());

            log.info("Análise concluída. id={} score={} level={} dataPartial={}",
                    savedAnalysis.getId(),
                    savedAnalysis.getScore(),
                    savedAnalysis.getRiskLevel(),
                    savedAnalysis.isDataPartial());

            return AnalysisResponse.from(savedAnalysis, false);

        } catch (Exception e) {
            idempotency.release(request.idempotencyKey());
            log.error("Falha na análise de risco para chave={}: {}", request.idempotencyKey(), e.getMessage());
            throw e;
        }
    }

    private void validateDuplicity(String nfeKey) {
        invoicePersistence.findByNfeKey(nfeKey).ifPresent(invoice -> {
            riskAnalysisPersistence.findTopByInvoiceIdOrderByCreatedAtDesc(invoice.getId())
                    .ifPresent(analysis -> {
                        throw new DuplicateInvoiceException(nfeKey, analysis.getId().toString());
                    });
        });
    }

    private ScoringContext buildScoringContext(NFeData nfeData, AnalysisRequest request) {
        return ScoringContext.builder()
                .nfeData(nfeData)
                .sefazStatus(SefazStatus.from(nfeData.getStatus()))
                .issuerCnpjStatus(CnpjStatus.ACTIVE)
                .payerCnpjStatus(CnpjStatus.ACTIVE)
                .requestedAdvanceValue(request.requestedAdvanceValue())
                .requestedMonthlyRate(request.requestedMonthlyRate())
                .hasUnavailableSource(false)
                .build();
    }
}
