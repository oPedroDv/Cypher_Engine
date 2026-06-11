package com.cypher.analysis.service;

import com.cypher.analysis.api.dto.AnalysisRequest;
import com.cypher.analysis.api.dto.AnalysisResponse;
import com.cypher.analysis.api.dto.StatisticsResponse;
import com.cypher.analysis.domain.FinancialMetrics;
import com.cypher.analysis.domain.Invoice;
import com.cypher.analysis.domain.NFeData;
import com.cypher.analysis.domain.NFeParser;
import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.domain.RiskFactor;
import com.cypher.analysis.domain.RiskLevel;
import com.cypher.analysis.engine.RiskEngineService;
import com.cypher.analysis.engine.ScoringContext;
import com.cypher.analysis.engine.SefazStatus;
import com.cypher.analysis.repository.InvoiceRepository;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.company.domain.CnpjStatus;
import com.cypher.company.service.CompanyService;
import com.cypher.outcome.domain.OutcomeType;
import com.cypher.outcome.repository.OutcomeRepository;
import com.cypher.shared.exception.DuplicateInvoiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final OutcomeRepository outcomeRepository;

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

            CnpjStatus issuerStatus = resolveStatus(nfeData.getIssuerCnpj(), tenantId);
            CnpjStatus payerStatus  = resolveStatus(nfeData.getRecipientCnpj(), tenantId);

            ScoringContext context = buildScoringContext(nfeData, request, issuerStatus, payerStatus, tenantId);
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

            Invoice invoice = Invoice.from(request.xmlBase64(), nfeData);
            Invoice savedInvoice = invoiceRepository.save(invoice);

            xmlStorageService.store(request.xmlBase64(), savedInvoice.getId(), nfeData.getAccessKey());

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

    @Transactional(readOnly = true)
    public Page<AnalysisResponse> listAnalyses(int page, int size, String riskLevel) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RiskAnalysis> result;
        if (riskLevel != null && !riskLevel.isBlank()) {
            RiskLevel level = RiskLevel.valueOf(riskLevel.toUpperCase());
            result = riskAnalysisRepository.findByRiskLevelOrderByCreatedAtDesc(level, pageable);
        } else {
            result = riskAnalysisRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return result.map(a -> AnalysisResponse.from(a, false));
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics() {
        long total   = riskAnalysisRepository.count();
        double avg   = riskAnalysisRepository.averageScore();

        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (RiskLevel level : RiskLevel.values()) {
            byLevel.put(level.name(), riskAnalysisRepository.countByRiskLevel(level));
        }

        long risksFound = byLevel.getOrDefault(RiskLevel.HIGH.name(), 0L)
                        + byLevel.getOrDefault(RiskLevel.CRITICAL.name(), 0L);

        List<StatisticsResponse.DailyCount> daily =
                riskAnalysisRepository.countPerDayLast30Days().stream()
                        .map(row -> new StatisticsResponse.DailyCount(
                                row[0].toString(),
                                ((Number) row[1]).longValue()
                        ))
                        .toList();

        return new StatisticsResponse(total, risksFound, avg, byLevel, daily);
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
            CnpjStatus payerStatus,
            UUID tenantId
    ) {
        String issuerCnpj = nfeData.getIssuerCnpj();
        String payerCnpj = nfeData.getRecipientCnpj();

        int issuerTotal = countIssuerInvoices(issuerCnpj);
        int payerTotal = countPayerInvoices(payerCnpj);
        int pairTotal = countPairInvoices(issuerCnpj, payerCnpj);

        EnumSet<OutcomeType> defaultOutcomes = EnumSet.of(OutcomeType.DEFAULT, OutcomeType.CANCELLED);
        int issuerDefaults = countIssuerOutcomes(issuerCnpj, tenantId, defaultOutcomes);
        int payerDefaults = countPayerOutcomes(payerCnpj, tenantId, defaultOutcomes);
        int pairDefaults = countPairOutcomes(issuerCnpj, payerCnpj, tenantId, defaultOutcomes);
        int payerLatePayments = countPayerLatePayments(payerCnpj, tenantId);
        BigDecimal issuerAvgValue = averageIssuerFaceValue(issuerCnpj);

        return ScoringContext.builder()
                .nfeData(nfeData)
                .sefazStatus(SefazStatus.from(nfeData.getStatus()))
                .issuerCnpjStatus(issuerStatus)
                .payerCnpjStatus(payerStatus)
                .issuerTotalInvoices(issuerTotal)
                .issuerDefaultCount(issuerDefaults)
                .issuerAvgValue(issuerAvgValue)
                .payerTotalInvoices(payerTotal)
                .payerLatePaymentCount(payerLatePayments)
                .payerDefaultCount(payerDefaults)
                .pairTotalInvoices(pairTotal)
                .pairDefaultCount(pairDefaults)
                .requestedAdvanceValue(request.requestedAdvanceValue())
                .requestedMonthlyRate(request.requestedMonthlyRate())
                .hasUnavailableSource(false)
                .build();
    }

    private int countIssuerInvoices(String issuerCnpj) {
        return isBlank(issuerCnpj) ? 0 : invoiceRepository.countByIssuerCnpj(issuerCnpj);
    }

    private int countPayerInvoices(String payerCnpj) {
        return isBlank(payerCnpj) ? 0 : invoiceRepository.countByRecipientCnpj(payerCnpj);
    }

    private int countPairInvoices(String issuerCnpj, String payerCnpj) {
        return isBlank(issuerCnpj) || isBlank(payerCnpj)
                ? 0
                : invoiceRepository.countByIssuerCnpjAndRecipientCnpj(issuerCnpj, payerCnpj);
    }

    private int countIssuerOutcomes(String issuerCnpj, UUID tenantId, EnumSet<OutcomeType> outcomeTypes) {
        return isBlank(issuerCnpj) || tenantId == null
                ? 0
                : outcomeRepository.countByIssuerCnpjAndOutcomeTypes(issuerCnpj, tenantId, outcomeTypes);
    }

    private int countPayerOutcomes(String payerCnpj, UUID tenantId, EnumSet<OutcomeType> outcomeTypes) {
        return isBlank(payerCnpj) || tenantId == null
                ? 0
                : outcomeRepository.countByPayerCnpjAndOutcomeTypes(payerCnpj, tenantId, outcomeTypes);
    }

    private int countPairOutcomes(String issuerCnpj, String payerCnpj, UUID tenantId, EnumSet<OutcomeType> outcomeTypes) {
        return isBlank(issuerCnpj) || isBlank(payerCnpj) || tenantId == null
                ? 0
                : outcomeRepository.countByPairAndOutcomeTypes(issuerCnpj, payerCnpj, tenantId, outcomeTypes);
    }

    private int countPayerLatePayments(String payerCnpj, UUID tenantId) {
        return isBlank(payerCnpj) || tenantId == null
                ? 0
                : outcomeRepository.countLateByPayerCnpj(payerCnpj, tenantId);
    }

    private BigDecimal averageIssuerFaceValue(String issuerCnpj) {
        if (isBlank(issuerCnpj)) {
            return null;
        }

        BigDecimal average = riskAnalysisRepository.averageFaceValueByIssuerCnpj(issuerCnpj);
        return average != null && average.compareTo(BigDecimal.ZERO) > 0 ? average : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
