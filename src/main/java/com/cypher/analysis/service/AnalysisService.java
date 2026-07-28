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
import com.cypher.shared.exception.InvalidFinancialParametersException;
import com.cypher.shared.exception.InvalidRequestException;
import com.cypher.audit.domain.AuditAction;
import com.cypher.audit.service.AuditService;
import com.cypher.infrastructure.web.CorrelationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
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

    private static final int MAX_PAGE_SIZE = 100;

    private final InvoiceRepository invoiceRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final RiskEngineService riskEngine;
    private final FinancialMetricsService financialMetricsService;
    private final XmlStorageService xmlStorageService;
    private final IdempotencyService idempotencyService;
    private final CompanyService companyService;
    private final OutcomeRepository outcomeRepository;
    private final SefazClient sefazClient;
    private final NFeParser nfeParser;
    private final InvoiceWriteService invoiceWriteService;
    private final AuditService auditService;

    public AnalysisResponse analyze(AnalysisRequest request, UUID tenantId) {
        requireTenant(tenantId);
        String correlationId = CorrelationContext.getOrCreate();
        String nfeKey = "UNKNOWN";
        boolean reservationOwned = false;
        try {
            NFeData nfeData = nfeParser.parse(request.xmlBase64());
            nfeKey = nfeData.getAccessKey();
            validateFinancialParameters(request, nfeData);

            String fingerprint = IdempotencyService.fingerprintOf(tenantId, nfeKey);
            String existingId = idempotencyService.checkOrReverse(
                    tenantId, request.idempotencyKey(), fingerprint);
            if (existingId != null) {
                log.debug("Hit idempotente key={} analysisId={}", request.idempotencyKey(), existingId);
                auditService.recordSuccess(AuditAction.ANALYSIS_IDEMPOTENT_HIT, "RiskAnalysis", existingId,
                        "Retorno idempotente", correlationId, tenantId);
                return riskAnalysisRepository.findByIdAndTenantId(UUID.fromString(existingId), tenantId)
                        .map(analysis -> AnalysisResponse.from(analysis, true))
                        .orElseThrow(() -> new IllegalStateException(
                                "Análise idempotente não encontrada: " + existingId));
            }
            reservationOwned = request.idempotencyKey() != null && !request.idempotencyKey().isBlank();

            validateDuplicity(tenantId, nfeData.getAccessKey());
            SefazClient.ConsultationResult sefazResult = sefazClient.consultStatus(nfeData.getAccessKey());

            CnpjResolution issuer = resolveStatus(nfeData.getIssuerCnpj(), tenantId);
            CnpjResolution payer  = resolveStatus(nfeData.getRecipientCnpj(), tenantId);
            CnpjStatus issuerStatus = issuer.status();
            CnpjStatus payerStatus  = payer.status();

            ScoringContext context = buildScoringContext(nfeData, request, sefazResult, issuer, payer, tenantId);
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


            InvoiceWriteService.PersistedAnalysis persisted = invoiceWriteService.persist(
                    Invoice.from(request.xmlBase64(), tenantId, nfeData), tenantId,
                    engineResult.score(),
                    engineResult.modelVersion(),
                    factors,
                    metrics,
                    engineResult.dataPartial()
            );
            Invoice savedInvoice = persisted.invoice();
            RiskAnalysis savedAnalysis = persisted.analysis();


            xmlStorageService.store(request.xmlBase64(), savedInvoice.getId(), nfeData.getAccessKey());
            idempotencyService.confirm(tenantId, request.idempotencyKey(),
                    savedAnalysis.getId().toString(), fingerprint);
            reservationOwned = false;
            auditService.recordSuccess(AuditAction.ANALYSIS_CREATED, "RiskAnalysis",
                    savedAnalysis.getId().toString(), "NF-e analisada via motor de risco",
                    correlationId, tenantId);

            log.info("Análise concluída id={} score={} level={} issuerStatus={} payerStatus={} dataPartial={}",
                    savedAnalysis.getId(),
                    savedAnalysis.getScore(),
                    savedAnalysis.getRiskLevel(),
                    issuerStatus,
                    payerStatus,
                    savedAnalysis.isDataPartial());

            return AnalysisResponse.from(savedAnalysis, false);

        } catch (Exception e) {
            log.error("Falha na análise key={}: {}", request.idempotencyKey(), e.getMessage(), e);
            if (reservationOwned) {
                runCleanup(e, "liberar reserva idempotente",
                        () -> idempotencyService.release(tenantId, request.idempotencyKey()));
            }
            String failureKey = nfeKey;
            runCleanup(e, "registrar auditoria de falha",
                    () -> auditService.recordFailure(AuditAction.ANALYSIS_FAILED, "Invoice", failureKey,
                            abbreviated(e.getMessage()), e.getClass().getSimpleName(), correlationId, tenantId));
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
        if (page < 0) {
            throw new InvalidRequestException("page deve ser maior ou igual a zero");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("size deve estar entre 1 e " + MAX_PAGE_SIZE);
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<RiskAnalysis> result;
        if (riskLevel != null && !riskLevel.isBlank()) {
            RiskLevel level = parseRiskLevel(riskLevel);
            result = riskAnalysisRepository.findByTenantIdAndRiskLevelOrderByCreatedAtDesc(tenantId, level, pageable);
        } else {
            result = riskAnalysisRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        }
        return result.map(a -> AnalysisResponse.from(a, false));
    }

    private RiskLevel parseRiskLevel(String riskLevel) {
        try {
            return RiskLevel.valueOf(riskLevel.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("riskLevel inválido: " + riskLevel);
        }
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

    private void validateFinancialParameters(AnalysisRequest request, NFeData nfeData) {
        BigDecimal requestedAdvance = request.requestedAdvanceValue();
        if (requestedAdvance != null && requestedAdvance.compareTo(nfeData.getTotalAmount()) > 0) {
            throw new InvalidFinancialParametersException(
                    "Valor de antecipação não pode ser maior que o valor total da NF-e");
        }
    }

    private CnpjResolution resolveStatus(String cnpj, UUID tenantId) {
        if (cnpj == null || cnpj.isBlank()) {
            log.warn("CNPJ nulo ou vazio — usando UNKNOWN para scoring");
            return CnpjResolution.unresolved();
        }
        try {
            return CnpjResolution.of(companyService.resolveCompany(cnpj, tenantId).getCnpjStatus());
        } catch (RuntimeException e) {
            log.warn("Falha ao resolver status do CNPJ={} — usando UNKNOWN e marcando análise como parcial. erro={}",
                    cnpj, e.getMessage(), e);
            return CnpjResolution.unresolved();
        }
    }

    /**
     * Resultado da consulta de cadastro de um CNPJ. {@code degraded} indica que o status não pôde ser
     * determinado e que a análise deve ser sinalizada como parcial.
     */
    private record CnpjResolution(CnpjStatus status, boolean degraded) {

        static CnpjResolution of(CnpjStatus status) {
            return new CnpjResolution(status, false);
        }

        static CnpjResolution unresolved() {
            return new CnpjResolution(CnpjStatus.UNKNOWN, true);
        }
    }

    private void runCleanup(Exception primary, String description, Runnable cleanup) {
        try {
            cleanup.run();
        } catch (RuntimeException cleanupFailure) {
            log.error("Falha ao {} durante o tratamento de erro: {}", description,
                    cleanupFailure.getMessage(), cleanupFailure);
            primary.addSuppressed(cleanupFailure);
        }
    }

    private ScoringContext buildScoringContext(
            NFeData nfeData,
            AnalysisRequest request,
            SefazClient.ConsultationResult sefazResult,
            CnpjResolution issuer,
            CnpjResolution payer,
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
                .issuerCnpjStatus(issuer.status())
                .payerCnpjStatus(payer.status())
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
                .hasUnavailableSource(sefazResult.sourceUnavailable() || issuer.degraded() || payer.degraded())
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

    private RiskLevel parseRiskLevel(String riskLevel) {
        try {
            return RiskLevel.valueOf(riskLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "riskLevel inválido: '%s'. Valores aceitos: %s".formatted(
                            riskLevel, Arrays.toString(RiskLevel.values())), e);
        }
    }

    private void requireTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("tenantId é obrigatório para operações multi-tenant");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String abbreviated(String message) {
        if (message == null) return "Falha sem mensagem";
        return message.length() <= 1024 ? message : message.substring(0, 1024);
    }
}
