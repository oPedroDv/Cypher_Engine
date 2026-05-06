package com.cypher.analysis.service;

import com.cypher.analysis.api.dto.AnalysisRequest;
import com.cypher.analysis.api.dto.AnalysisResponse;
import com.cypher.analysis.domain.*;
import com.cypher.analysis.engine.RiskEngineService;
import com.cypher.analysis.engine.ScoringContext;
import com.cypher.analysis.repository.InvoiceRepository;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.shared.exception.DuplicateInvoiceException;
import com.cypher.shared.exception.InvalidNFeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
            IdempotencyService idempotencyService
    ) {
        this.invoiceRepository      = invoiceRepository;
        this.riskRepository         = riskRepository;
        this.riskEngine             = riskEngine;
        this.financialMetricsService = financialMetricsService;
        this.xmlStorageService      = xmlStorageService;
        this.idempotencyService     = idempotencyService;
    }

    @Transactional
    public AnalysisResponse analyze(AnalysisRequest request) {

        String existingId = idempotencyService.checkOrReserve(request.idempotencyKey());
        if (existingId != null) {
            log.debug("Requisição idempotente. Retornando análise existente: {}", existingId);
            return riskRepository.findById(UUID.fromString(existingId))
                    .map(a -> AnalysisResponse.from(a, true))
                    .orElseThrow(() -> new IllegalStateException(
                            "Análise idempotente não encontrada no banco: " + existingId));
        }

        try {
            NFeData nfeData = parseXml(request.xmlBase64());

            checkDuplicate(nfeData.getChaveAcesso());
            Invoice invoice = Invoice.of(request.xmlBase64());
            invoice = invoiceRepository.save(invoice);

            xmlStorageService.store(request.xmlBase64(), invoice.getId(), nfeData.getChaveAcesso());

            ScoringContext context = buildScoringContext(nfeData, request);

            RiskEngineService.EngineResult engineResult = riskEngine.score(context);

            FinancialMetrics metrics = financialMetricsService.calculate(
                    nfeData.getValorTotal(),
                    request.requestedAdvanceValue(),
                    request.requestedMonthlyRate(),
                    engineResult.score()
            );

            List<RiskFactor> factors = engineResult.factors().stream()
                    .map(RiskFactor::from)
                    .toList();

            RiskAnalysis analysis = RiskAnalysis.of(
                    invoice,
                    engineResult.score(),
                    engineResult.modelVersion(),
                    factors,
                    metrics,
                    engineResult.dataIsPartial()
            );
            riskRepository.save(analysis);

            idempotencyService.complete(request.idempotencyKey(), analysis.getId());

            log.info("Análise concluída. ID: {} Score: {} Level: {} Partial: {}",
                    analysis.getId(),
                    engineResult.score(),
                    RiskLevel.from(engineResult.score()),
                    engineResult.dataIsPartial());

            return AnalysisResponse.from(analysis, false);

        } catch (Exception e) {
            idempotencyService.release(request.idempotencyKey());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Optional<AnalysisResponse> findById(UUID id) {
        return riskRepository.findById(id)
                .map(a -> AnalysisResponse.from(a, false));
    }

    private NFeData parseXml(String xmlBase64) {
        try {

            return NFeData.builder()
                    .chaveAcesso(extractChaveFromXml(xmlBase64))
                    .valorTotal(extractValorFromXml(xmlBase64))
                    .buildUnsafe();
        } catch (Exception e) {
            throw new InvalidNFeException("Falha ao processar o XML: " + e.getMessage(), e);
        }
    }

    private void checkDuplicate(String chaveNfe) {
        if (chaveNfe == null) return;

        invoiceRepository.findByChaveNfe(chaveNfe).ifPresent(existing -> {
            riskRepository.findTopByInvoiceIdOrderByCreatedAtDesc(existing.getId())
                    .ifPresent(analysis -> {
                        throw new DuplicateInvoiceException(
                                chaveNfe,
                                analysis.getId().toString()
                        );
                    });
        });
    }

    private ScoringContext buildScoringContext(NFeData nfeData, AnalysisRequest request) {

        return ScoringContext.builder()
                .nfeData(nfeData)
                .sefazStatus("AUTHORIZED")
                .issuerCnpjStatus("ACTIVE")
                .payerCnpjStatus("ACTIVE")
                .issuerTotalInvoices(0)
                .issuerDefaultCount(0)
                .issuerAvgValue(BigDecimal.ZERO)
                .payerTotalInvoices(0)
                .payerDefaultCount(0)
                .payerLatePaymentCount(0)
                .pairTotalInvoices(0)
                .pairDefaultCount(0)
                .requestedAdvanceValue(
                        request.requestedAdvanceValue() != null
                                ? request.requestedAdvanceValue()
                                : BigDecimal.ZERO)
                .requestedMonthlyRate(request.requestedMonthlyRate())
                .hasUnavailableSource(false)
                .build();
    }
    private String extractChaveFromXml(String xmlBase64) {
        return "NFe" + UUID.randomUUID().toString().replace("-", "").substring(0, 41);
    }

    private BigDecimal extractValorFromXml(String xmlBase64) {
        return new BigDecimal("10000.00");
    }
}