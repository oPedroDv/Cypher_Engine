package com.cypher.analysis.service;

import com.cypher.analysis.api.dto.AnalysisRequest;
import com.cypher.analysis.api.dto.AnalysisResponse;
import com.cypher.analysis.domain.FinancialMetrics;
import com.cypher.analysis.domain.Invoice;
import com.cypher.analysis.domain.NFeData;
import com.cypher.analysis.domain.NFeParser;
import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.domain.RiskFactor;
import com.cypher.analysis.engine.RiskEngineService;
import com.cypher.analysis.engine.ScoringContext;
import com.cypher.analysis.engine.SefazStatus;
import com.cypher.analysis.repository.InvoiceRepository;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.company.domain.CnpjStatus;
import com.cypher.company.service.CompanyService;
import com.cypher.shared.exception.DuplicateInvoiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final InvoiceRepository invoiceRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final RiskEngineService riskEngine;
    private final FinancialMetricsService financialMetricsService;
    private final XmlStorageService xmlStorageService;
    private final IdempotencyService idempotencyService;
    private final CompanyService companyService;

    @Transactional
    public AnalysisResponse analyze(AnalysisRequest request, UUID tenantId) {

        String existingId = idempotencyService.checkOrReverse(request.idempotencyKey());
        if (existingId != null) {
            log.debug("Hit idempotente key={} analysisId={}", request.idempotencyKey(), existingId);
            return riskAnalysisRepository.findById(UUID.fromString(existingId))
                    .map(analysis -> AnalysisResponse.from(analysis, true))
                    .orElseThrow(() -> new IllegalStateException(
                            "Análise idempotente não encontrada: " + existingId));
        }

        try {
            NFeData nfeData = NFeParser.parse(request.xmlBase64());

            validateDuplicity(nfeData.getAccessKey());

            Invoice invoice = Invoice.of(request.xmlBase64(), nfeData.getAccessKey());
            Invoice savedInvoice = invoiceRepository.save(invoice);

            xmlStorageService.store(request.xmlBase64(), savedInvoice.getId(), nfeData.getAccessKey());

            CnpjStatus issuerStatus = resolveStatus(nfeData.getIssuerCnpj(), tenantId);
            CnpjStatus payerStatus  = resolveStatus(nfeData.getRecipientCnpj(), tenantId);

            ScoringContext context = buildScoringContext(nfeData, request, issuerStatus, payerStatus);
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

            RiskAnalysis savedAnalysis = riskAnalysisRepository.save(analysis);

            idempotencyService.confirm(request.idempotencyKey(), savedAnalysis.getId().toString());

            log.info("Análise concluída id={} score={} level={} issuerStatus={} payerStatus={} dataPartial={}",
                    savedAnalysis.getId(),
                    savedAnalysis.getScore(),
                    savedAnalysis.getRiskLevel(),
                    issuerStatus,
                    payerStatus,
                    savedAnalysis.isDataPartial());

            return AnalysisResponse.from(savedAnalysis, false);

        } catch (Exception e) {
            idempotencyService.release(request.idempotencyKey());
            log.error("Falha na análise key={}: {}", request.idempotencyKey(), e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Optional<AnalysisResponse> findById(UUID id) {
        return riskAnalysisRepository.findById(id)
                .map(analysis -> AnalysisResponse.from(analysis, false));
    }

    private void validateDuplicity(String accessKey) {
        invoiceRepository.findByNfeKey(accessKey).ifPresent(invoice ->
                riskAnalysisRepository.findTopByInvoiceIdOrderByCreatedAtDesc(invoice.getId())
                        .ifPresent(analysis -> {
                            throw new DuplicateInvoiceException(accessKey, analysis.getId().toString());
                        })
        );
    }

    private CnpjStatus resolveStatus(String cnpj, UUID tenantId) {
        if (cnpj == null || cnpj.isBlank()) {
            log.warn("CNPJ nulo ou vazio — usando UNKNOWN para scoring");
            return CnpjStatus.UNKNOWN;
        }
        try {
            return companyService.resolveCompany(cnpj, tenantId).getCnpjStatus();
        } catch (Exception e) {
            log.warn("Falha ao resolver status do CNPJ={} — usando UNKNOWN. erro={}", cnpj, e.getMessage());
            return CnpjStatus.UNKNOWN;
        }
    }

    private ScoringContext buildScoringContext(
            NFeData nfeData,
            AnalysisRequest request,
            CnpjStatus issuerStatus,
            CnpjStatus payerStatus
    ) {
        return ScoringContext.builder()
                .nfeData(nfeData)
                .sefazStatus(SefazStatus.from(nfeData.getStatus()))
                .issuerCnpjStatus(issuerStatus)
                .payerCnpjStatus(payerStatus)
                .requestedAdvanceValue(request.requestedAdvanceValue())
                .requestedMonthlyRate(request.requestedMonthlyRate())
                .hasUnavailableSource(false)
                .build();
    }
}