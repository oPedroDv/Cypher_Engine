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
import com.cypher.analysis.repository.InvoiceHistoryStats;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.company.domain.CnpjStatus;
import com.cypher.company.service.CompanyService;
import com.cypher.outcome.domain.OutcomeType;
import com.cypher.outcome.repository.OutcomeHistoryStats;
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
    private final SefazClient sefazClient;

    @Transactional
    public AnalysisResponse analyze(AnalysisRequest request, UUID tenantId) {
        requireTenant(tenantId);

        String existingId = idempotencyService.checkOrReverse(tenantId, request.idempotencyKey());
        if (existingId != null) {
            log.debug("Hit idempotente key={} analysisId={}", request.idempotencyKey(), existingId);
            return riskAnalysisRepository.findByIdAndTenantId(UUID.fromString(existingId), tenantId)
                    .map(analysis -> AnalysisResponse.from(analysis, true))
                    .orElseThrow(() -> new IllegalStateException(
                            "Análise idempotente não encontrada: " + existingId));
        }

        try {
            NFeData nfeData = NFeParser.parse(request.xmlBase64());

            validateDuplicity(tenantId, nfeData.getAccessKey());
            SefazClient.ConsultationResult sefazResult = sefazClient.consultStatus(nfeData.getAccessKey());

            CnpjStatus issuerStatus = resolveStatus(nfeData.getIssuerCnpj(), tenantId);
            CnpjStatus payerStatus  = resolveStatus(nfeData.getRecipientCnpj(), tenantId);

            ScoringContext context = buildScoringContext(nfeData, request, sefazResult, issuerStatus, payerStatus, tenantId);
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

            Invoice invoice = Invoice.from(request.xmlBase64(), tenantId, nfeData);
            Invoice savedInvoice = invoiceRepository.save(invoice);

            xmlStorageService.store(request.xmlBase64(), savedInvoice.getId(), nfeData.getAccessKey());

            RiskAnalysis analysis = RiskAnalysis.of(
                    savedInvoice,
                    tenantId,
                    engineResult.score(),
                    engineResult.modelVersion(),
                    factors,
                    metrics,
                    engineResult.dataPartial()
            );

            RiskAnalysis savedAnalysis = riskAnalysisRepository.save(analysis);

            idempotencyService.confirm(tenantId, request.idempotencyKey(), savedAnalysis.getId().toString());

            log.info("Análise concluída id={} score={} level={} issuerStatus={} payerStatus={} dataPartial={}",
                    savedAnalysis.getId(),
                    savedAnalysis.getScore(),
                    savedAnalysis.getRiskLevel(),
                    issuerStatus,
                    payerStatus,
                    savedAnalysis.isDataPartial());

            return AnalysisResponse.from(savedAnalysis, false);

        } catch (Exception e) {
            idempotencyService.release(tenantId, request.idempotencyKey());
            log.error("Falha na análise key={}: {}", request.idempotencyKey(), e.getMessage());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Optional<AnalysisResponse> findById(UUID id, UUID tenantId) {
        requireTenant(tenantId);
        return riskAnalysisRepository.findByIdAndTenantId(id, tenantId)
                .map(analysis -> AnalysisResponse.from(analysis, false));
    }

    @Transactional(readOnly = true)
    public Page<AnalysisResponse> listAnalyses(UUID tenantId, int page, int size, String riskLevel) {
        requireTenant(tenantId);
        Pageable pageable = PageRequest.of(page, size);
        Page<RiskAnalysis> result;
        if (riskLevel != null && !riskLevel.isBlank()) {
            RiskLevel level = RiskLevel.valueOf(riskLevel.toUpperCase());
            result = riskAnalysisRepository.findByTenantIdAndRiskLevelOrderByCreatedAtDesc(tenantId, level, pageable);
        } else {
            result = riskAnalysisRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        }
        return result.map(a -> AnalysisResponse.from(a, false));
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics(UUID tenantId) {
        requireTenant(tenantId);
        long total   = riskAnalysisRepository.countByTenantId(tenantId);
        double avg   = riskAnalysisRepository.averageScoreByTenant(tenantId);

        Map<String, Long> byLevel = new LinkedHashMap<>();
        for (RiskLevel level : RiskLevel.values()) {
            byLevel.put(level.name(), riskAnalysisRepository.countByTenantIdAndRiskLevel(tenantId, level));
        }

        long risksFound = byLevel.getOrDefault(RiskLevel.HIGH.name(), 0L)
                        + byLevel.getOrDefault(RiskLevel.CRITICAL.name(), 0L);

        List<StatisticsResponse.DailyCount> daily =
                riskAnalysisRepository.countPerDayLast30Days(tenantId).stream()
                        .map(row -> new StatisticsResponse.DailyCount(
                                row[0].toString(),
                                ((Number) row[1]).longValue()
                        ))
                        .toList();

        return new StatisticsResponse(total, risksFound, avg, byLevel, daily);
    }

    private void validateDuplicity(UUID tenantId, String accessKey) {
        invoiceRepository.findByTenantIdAndNfeKey(tenantId, accessKey).ifPresent(invoice ->
                riskAnalysisRepository.findTopByTenantIdAndInvoiceIdOrderByCreatedAtDesc(tenantId, invoice.getId())
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
            SefazClient.ConsultationResult sefazResult,
            CnpjStatus issuerStatus,
            CnpjStatus payerStatus,
            UUID tenantId
    ) {
        String issuerCnpj = nfeData.getIssuerCnpj();
        String payerCnpj = nfeData.getRecipientCnpj();

        InvoiceHistoryStats invoiceStats = summarizeInvoiceHistory(tenantId, issuerCnpj, payerCnpj);
        EnumSet<OutcomeType> defaultOutcomes = EnumSet.of(OutcomeType.DEFAULT, OutcomeType.CANCELLED);
        OutcomeHistoryStats outcomeStats = summarizeOutcomeHistory(issuerCnpj, payerCnpj, tenantId, defaultOutcomes);

        return ScoringContext.builder()
                .nfeData(nfeData)
                .sefazStatus(SefazStatus.from(sefazResult.status(), sefazResult.notConfigured()))
                .issuerCnpjStatus(issuerStatus)
                .payerCnpjStatus(payerStatus)
                .issuerTotalInvoices(invoiceStats.issuerTotalAsInt())
                .issuerDefaultCount(outcomeStats.issuerDefaultsAsInt())
                .issuerAvgValue(invoiceStats.issuerAvgValue())
                .payerTotalInvoices(invoiceStats.payerTotalAsInt())
                .payerLatePaymentCount(outcomeStats.payerLatePaymentsAsInt())
                .payerDefaultCount(outcomeStats.payerDefaultsAsInt())
                .pairTotalInvoices(invoiceStats.pairTotalAsInt())
                .pairDefaultCount(outcomeStats.pairDefaultsAsInt())
                .requestedAdvanceValue(request.requestedAdvanceValue())
                .requestedMonthlyRate(request.requestedMonthlyRate())
                .hasUnavailableSource(sefazResult.sourceUnavailable())
                .build();
    }

    private InvoiceHistoryStats summarizeInvoiceHistory(UUID tenantId, String issuerCnpj, String payerCnpj) {
        if (isBlank(issuerCnpj) || isBlank(payerCnpj)) {
            return emptyInvoiceHistoryStats();
        }

        InvoiceHistoryStats stats = invoiceRepository.summarizeHistory(tenantId, issuerCnpj, payerCnpj);
        return stats != null ? stats : emptyInvoiceHistoryStats();
    }

    private OutcomeHistoryStats summarizeOutcomeHistory(
            String issuerCnpj,
            String payerCnpj,
            UUID tenantId,
            EnumSet<OutcomeType> outcomeTypes
    ) {
        if (isBlank(issuerCnpj) || isBlank(payerCnpj) || tenantId == null) {
            return new OutcomeHistoryStats(0, 0, 0, 0);
        }

        OutcomeHistoryStats stats = outcomeRepository.summarizeHistory(issuerCnpj, payerCnpj, tenantId, outcomeTypes);
        return stats != null ? stats : new OutcomeHistoryStats(0, 0, 0, 0);
    }

    private InvoiceHistoryStats emptyInvoiceHistoryStats() {
        return new InvoiceHistoryStats() {
            @Override
            public Number getIssuerTotal() {
                return 0;
            }

            @Override
            public Number getPayerTotal() {
                return 0;
            }

            @Override
            public Number getPairTotal() {
                return 0;
            }

            @Override
            public BigDecimal getIssuerAvgValue() {
                return null;
            }
        };
    }

    private void requireTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("tenantId é obrigatório para operações multi-tenant");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
