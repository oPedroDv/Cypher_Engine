package com.cypher.analysis.service;

import com.cypher.analysis.api.dto.AnalysisRequest;
import com.cypher.analysis.api.dto.AnalysisResponse;
import com.cypher.analysis.application.AnalyzeInvoiceUseCase;
import com.cypher.analysis.application.port.InvoicePersistencePort;
import com.cypher.analysis.application.port.RiskAnalysisPersistencePort;
import com.cypher.analysis.domain.FinancialMetrics;
import com.cypher.analysis.domain.Invoice;
import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.engine.ScoringContext;
import com.cypher.analysis.engine.RiskEngineService;
import com.cypher.analysis.engine.rules.RuleResult;
import com.cypher.shared.exception.DuplicateInvoiceException;
import com.cypher.shared.exception.InvalidNFeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    private static final String CHAVE_NFE = "12345678901234567890123456789012345678901234";
    private static final String XML = "<nfe><chNFe>" + CHAVE_NFE + "</chNFe><vNF>10000.0</vNF></nfe>";

    @Mock
    private InvoicePersistencePort invoiceRepository;

    @Mock
    private RiskAnalysisPersistencePort riskRepository;

    @Mock
    private RiskEngineService riskEngine;

    @Mock
    private FinancialMetricsService financialMetricsService;

    @Mock
    private XmlStorageService xmlStorageService;

    @Mock
    private IdempotencyService idempotencyService;

    private AnalysisService service;

    @BeforeEach
    void setUp() {
        AnalyzeInvoiceUseCase analyzeInvoiceUseCase = new AnalyzeInvoiceUseCase(
                invoiceRepository,
                riskRepository,
                riskEngine,
                financialMetricsService,
                xmlStorageService,
                idempotencyService
        );

        service = new AnalysisService(
                analyzeInvoiceUseCase,
                riskRepository
        );
    }

    @Test
    void analyzeCreatesInvoiceStoresXmlScoresRiskAndConfirmsIdempotency() {
        UUID invoiceId = UUID.randomUUID();
        UUID analysisId = UUID.randomUUID();
        AnalysisRequest request = request("idem-1");
        RuleResult factor = RuleResult.of("sefaz", "DOCUMENT", 0.25, 0.4, "INCREASE", "ok", "SEFAZ");
        FinancialMetrics metrics = FinancialMetrics.calculate(
                new BigDecimal("10000.00"),
                new BigDecimal("8500.00"),
                3.2,
                0.25
        );

        when(idempotencyService.checkOrReverse("idem-1")).thenReturn(null);
        when(invoiceRepository.findByNfeKey(CHAVE_NFE)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            ReflectionTestUtils.setField(invoice, "id", invoiceId);
            return invoice;
        });
        when(riskEngine.score(any())).thenReturn(new RiskEngineService.EngineResult(
                0.25,
                List.of(factor),
                "model-v1",
                false
        ));
        when(financialMetricsService.calculate(
                new BigDecimal("10000.0"),
                new BigDecimal("8500.00"),
                3.2,
                0.25
        )).thenReturn(metrics);
        when(riskRepository.save(any(RiskAnalysis.class))).thenAnswer(invocation -> {
            RiskAnalysis analysis = invocation.getArgument(0);
            ReflectionTestUtils.setField(analysis, "id", analysisId);
            ReflectionTestUtils.setField(analysis, "createdAt", Instant.parse("2026-05-27T12:00:00Z"));
            return analysis;
        });

        AnalysisResponse response = service.analyze(request);

        assertThat(response.analysisId()).isEqualTo(analysisId);
        assertThat(response.invoiceId()).isEqualTo(invoiceId);
        assertThat(response.idempotent()).isFalse();
        assertThat(response.score()).isEqualTo(0.25);
        assertThat(response.modelVersion()).isEqualTo("model-v1");
        assertThat(response.factors()).hasSize(1);
        assertThat(response.financial().requestedAdvanceValue()).isEqualByComparingTo("8500.00");

        verify(xmlStorageService).store(XML, invoiceId, CHAVE_NFE);
        verify(idempotencyService).confirm("idem-1", analysisId.toString());

        ArgumentCaptor<ScoringContext> contextCaptor = ArgumentCaptor.forClass(ScoringContext.class);
        verify(riskEngine).score(contextCaptor.capture());
        assertThat(contextCaptor.getValue().nfeData().getAccessKey()).isEqualTo(CHAVE_NFE);
        assertThat(contextCaptor.getValue().requestedAdvanceValue()).isEqualByComparingTo("8500.00");
        assertThat(contextCaptor.getValue().requestedMonthlyRate()).isEqualTo(3.2);

        ArgumentCaptor<RiskAnalysis> analysisCaptor = ArgumentCaptor.forClass(RiskAnalysis.class);
        verify(riskRepository).save(analysisCaptor.capture());
        assertThat(analysisCaptor.getValue().getInvoice().getNfeKey()).isEqualTo(CHAVE_NFE);
        assertThat(analysisCaptor.getValue().getScore()).isEqualTo(0.25);
        assertThat(analysisCaptor.getValue().getModelVersion()).isEqualTo("model-v1");
        assertThat(analysisCaptor.getValue().getFinancialMetrics()).isSameAs(metrics);
        assertThat(analysisCaptor.getValue().getFactors()).hasSize(1);
    }

    @Test
    void analyzeReturnsExistingAnalysisWhenIdempotencyKeyWasAlreadyConfirmed() {
        UUID analysisId = UUID.randomUUID();
        RiskAnalysis existing = persistedAnalysis(analysisId, UUID.randomUUID());
        when(idempotencyService.checkOrReverse("idem-1")).thenReturn(analysisId.toString());
        when(riskRepository.findById(analysisId)).thenReturn(Optional.of(existing));

        AnalysisResponse response = service.analyze(request("idem-1"));

        assertThat(response.analysisId()).isEqualTo(analysisId);
        assertThat(response.idempotent()).isTrue();
        verifyNoInteractions(invoiceRepository, riskEngine, financialMetricsService, xmlStorageService);
        verify(riskRepository, never()).save(any());
        verify(idempotencyService, never()).confirm(any(), any());
    }

    @Test
    void analyzeReleasesIdempotencyKeyWhenDuplicateInvoiceIsDetected() {
        UUID invoiceId = UUID.randomUUID();
        Invoice existingInvoice = invoice(invoiceId);
        RiskAnalysis existingAnalysis = persistedAnalysis(UUID.randomUUID(), invoiceId);

        when(idempotencyService.checkOrReverse("idem-1")).thenReturn(null);
        when(invoiceRepository.findByNfeKey(CHAVE_NFE)).thenReturn(Optional.of(existingInvoice));
        when(riskRepository.findTopByInvoiceIdOrderByCreatedAtDesc(invoiceId)).thenReturn(Optional.of(existingAnalysis));

        assertThatThrownBy(() -> service.analyze(request("idem-1")))
                .isInstanceOf(DuplicateInvoiceException.class)
                .hasMessageContaining(CHAVE_NFE)
                .hasMessageContaining(existingAnalysis.getId().toString());

        verify(idempotencyService).release("idem-1");
        verify(invoiceRepository, never()).save(any());
        verifyNoInteractions(riskEngine, financialMetricsService, xmlStorageService);
    }

    @Test
    void analyzeReleasesIdempotencyAndDoesNotPersistAnalysisWhenRiskEngineFails() {
        UUID invoiceId = UUID.randomUUID();
        AnalysisRequest request = request("idem-1");

        when(idempotencyService.checkOrReverse("idem-1")).thenReturn(null);
        when(invoiceRepository.findByNfeKey(CHAVE_NFE)).thenReturn(Optional.empty());
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice invoice = invocation.getArgument(0);
            ReflectionTestUtils.setField(invoice, "id", invoiceId);
            return invoice;
        });
        when(riskEngine.score(any())).thenThrow(new IllegalStateException("engine unavailable"));

        assertThatThrownBy(() -> service.analyze(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("engine unavailable");

        verify(xmlStorageService).store(XML, invoiceId, CHAVE_NFE);
        verify(idempotencyService).release("idem-1");
        verify(riskRepository, never()).save(any());
        verify(idempotencyService, never()).confirm(any(), any());
        verifyNoInteractions(financialMetricsService);
    }

    @Test
    void analyzeReleasesIdempotencyAndSkipsPersistenceWhenNFeIsInvalid() {
        AnalysisRequest request = new AnalysisRequest(" ", "idem-1", new BigDecimal("8500.00"), 3.2);
        when(idempotencyService.checkOrReverse("idem-1")).thenReturn(null);

        assertThatThrownBy(() -> service.analyze(request))
                .isInstanceOf(InvalidNFeException.class)
                .hasMessageContaining("XML da NF-e");

        verify(idempotencyService).release("idem-1");
        verifyNoInteractions(invoiceRepository, riskRepository, riskEngine, financialMetricsService, xmlStorageService);
        verify(idempotencyService, never()).confirm(any(), any());
    }

    @Test
    void analyzeThrowsWhenIdempotentResultCannotBeLoaded() {
        UUID missingAnalysisId = UUID.randomUUID();
        when(idempotencyService.checkOrReverse("idem-1")).thenReturn(missingAnalysisId.toString());
        when(riskRepository.findById(missingAnalysisId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyze(request("idem-1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Erro ao recuperar análise idempotente");

        verifyNoInteractions(invoiceRepository, riskEngine, financialMetricsService, xmlStorageService);
        verify(riskRepository, never()).save(any());
        verify(idempotencyService, never()).confirm(any(), any());
        verify(idempotencyService, never()).release(any());
    }

    @Test
    void findByIdMapsPersistedAnalysisToResponse() {
        UUID analysisId = UUID.randomUUID();
        RiskAnalysis existing = persistedAnalysis(analysisId, UUID.randomUUID());
        when(riskRepository.findById(analysisId)).thenReturn(Optional.of(existing));

        Optional<AnalysisResponse> response = service.findById(analysisId);

        assertThat(response).isPresent();
        assertThat(response.get().analysisId()).isEqualTo(analysisId);
        assertThat(response.get().idempotent()).isTrue();
    }

    private static AnalysisRequest request(String idempotencyKey) {
        return new AnalysisRequest(XML, idempotencyKey, new BigDecimal("8500.00"), 3.2);
    }

    private static Invoice invoice(UUID invoiceId) {
        Invoice invoice = Invoice.of(XML, CHAVE_NFE);
        ReflectionTestUtils.setField(invoice, "id", invoiceId);
        return invoice;
    }

    private static RiskAnalysis persistedAnalysis(UUID analysisId, UUID invoiceId) {
        RiskAnalysis analysis = RiskAnalysis.of(
                invoice(invoiceId),
                0.25,
                "model-v1",
                List.of(RuleResult.of("sefaz", "DOCUMENT", 0.25, 0.4, "INCREASE", "ok", "SEFAZ"))
                        .stream()
                        .map(com.cypher.analysis.domain.RiskFactor::from)
                        .toList(),
                FinancialMetrics.calculate(new BigDecimal("10000.00"), new BigDecimal("8500.00"), 3.2, 0.25),
                false
        );
        ReflectionTestUtils.setField(analysis, "id", analysisId);
        ReflectionTestUtils.setField(analysis, "createdAt", Instant.parse("2026-05-27T12:00:00Z"));
        return analysis;
    }
}
