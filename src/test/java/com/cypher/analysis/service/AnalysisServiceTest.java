package com.cypher.analysis.service;

import com.cypher.analysis.api.dto.AnalysisRequest;
import com.cypher.analysis.api.dto.AnalysisResponse;
import com.cypher.analysis.domain.*;
import com.cypher.analysis.engine.RiskEngineService;
import com.cypher.analysis.engine.ScoringContext;
import com.cypher.analysis.engine.rules.RuleResult;
import com.cypher.analysis.repository.InvoiceRepository;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.company.domain.CnpjStatus;
import com.cypher.company.domain.Company;
import com.cypher.company.service.CompanyService;
import com.cypher.outcome.repository.OutcomeRepository;
import com.cypher.shared.exception.DuplicateInvoiceException;
import com.cypher.shared.exception.InvalidNFeException;
import com.cypher.shared.exception.InvalidFinancialParametersException;
import com.cypher.testutil.TestEntityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    private static final String CHAVE_NFE = "12345678901234567890123456789012345678901234";
    private static final String XML = """
            <nfe>
              <chNFe>%s</chNFe>
              <emit><CNPJ>00000000000000</CNPJ><xNome>Emitente Teste</xNome></emit>
              <dest><CNPJ>11111111111111</CNPJ><xNome>Destinatario Teste</xNome></dest>
              <vNF>10000.0</vNF>
              <dhEmi>2026-06-01T10:00:00-03:00</dhEmi>
              <dVenc>2026-07-01</dVenc>
            </nfe>
            """.formatted(CHAVE_NFE);
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String IDEM_KEY = "idem-1";

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private RiskAnalysisRepository riskRepository;

    @Mock
    private RiskEngineService riskEngine;

    @Mock
    private FinancialMetricsService financialMetricsService;

    @Mock
    private XmlStorageService xmlStorageService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private CompanyService companyService;

    @Mock
    private OutcomeRepository outcomeRepository;

    @Mock
    private SefazClient sefazClient;

    @Mock
    private NFeParser nfeParser;

    @InjectMocks
    private AnalysisService service;

    @BeforeEach
    void setUpParser() {
        lenient().when(nfeParser.parse(XML)).thenReturn(NFeData.builder()
                .accessKey(CHAVE_NFE)
                .issuerCnpj("00000000000000")
                .issuerLegalName("Emitente Teste")
                .recipientCnpj("11111111111111")
                .recipientLegalName("Destinatario Teste")
                .totalAmount(new BigDecimal("10000.00"))
                .productsAmount(new BigDecimal("10000.00"))
                .freightAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .issueDate(java.time.LocalDate.of(2026, 6, 1))
                .dueDate(java.time.LocalDate.of(2026, 7, 1))
                .status(InvoiceStatus.PENDING)
                .build());
    }

    @Nested
    @DisplayName("analyze")
    class Analyze {

        @Test
        @DisplayName("Should successfully analyze a new invoice")
        void shouldAnalyzeNewInvoice() {
            // Given
            UUID invoiceId = UUID.randomUUID();
            UUID analysisId = UUID.randomUUID();
            AnalysisRequest request = createRequest();
            
            FinancialMetrics metrics = createMetrics();
            RuleResult engineFactor = RuleResult.of("sefaz", "DOCUMENT", 0.25, 0.4, "INCREASE", "ok", "SEFAZ");

            when(idempotencyService.checkOrReverse(TENANT_ID, IDEM_KEY)).thenReturn(null);
            when(invoiceRepository.findByTenantIdAndNfeKey(TENANT_ID, CHAVE_NFE)).thenReturn(Optional.empty());
            when(sefazClient.consultStatus(CHAVE_NFE)).thenReturn(
                    SefazClient.ConsultationResult.available(InvoiceStatus.AUTHORIZED, "SEFAZ", "Autorizada")
            );
            when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(inv -> TestEntityHelper.withId(inv.getArgument(0), invoiceId));
            
            when(companyService.resolveCompany(any(), eq(TENANT_ID))).thenReturn(mock(Company.class));
            
            when(riskEngine.score(any(ScoringContext.class))).thenReturn(new RiskEngineService.EngineResult(
                    0.25, List.of(engineFactor), "model-v1", false));
            
            when(financialMetricsService.calculate(any(), any(), anyDouble(), anyDouble())).thenReturn(metrics);
            
            when(riskRepository.save(any(RiskAnalysis.class))).thenAnswer(inv -> {
                RiskAnalysis analysis = inv.getArgument(0);
                TestEntityHelper.withId(analysis, analysisId);
                TestEntityHelper.withCreatedAt(analysis, Instant.now());
                return analysis;
            });

            // When
            AnalysisResponse response = service.analyze(request, TENANT_ID);

            // Then
            assertThat(response.analysisId()).isEqualTo(analysisId);
            assertThat(response.invoiceId()).isEqualTo(invoiceId);
            assertThat(response.idempotent()).isFalse();
            
            verify(xmlStorageService).store(XML, invoiceId, CHAVE_NFE);
            verify(idempotencyService).confirmAfterCommit(TENANT_ID, IDEM_KEY, analysisId.toString());
            
            ArgumentCaptor<ScoringContext> contextCaptor = ArgumentCaptor.forClass(ScoringContext.class);
            verify(riskEngine).score(contextCaptor.capture());
            assertThat(contextCaptor.getValue().nfeData().getAccessKey()).isEqualTo(CHAVE_NFE);
            assertThat(contextCaptor.getValue().sefazStatus()).isEqualTo(com.cypher.analysis.engine.SefazStatus.AUTHORIZED);
            assertThat(contextCaptor.getValue().hasUnavailableSource()).isFalse();

            ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
            verify(invoiceRepository).saveAndFlush(invoiceCaptor.capture());
            assertThat(invoiceCaptor.getValue().getIssuerCnpj()).isEqualTo("00000000000000");
        }

        @Test
        @DisplayName("Should return existing analysis for idempotent request")
        void shouldReturnExistingAnalysis() {
            UUID analysisId = UUID.randomUUID();
            RiskAnalysis existing = createPersistedAnalysis(analysisId);
            
            when(idempotencyService.checkOrReverse(TENANT_ID, IDEM_KEY)).thenReturn(analysisId.toString());
            when(riskRepository.findByIdAndTenantId(analysisId, TENANT_ID)).thenReturn(Optional.of(existing));

            AnalysisResponse response = service.analyze(createRequest(), TENANT_ID);

            assertThat(response.analysisId()).isEqualTo(analysisId);
            assertThat(response.idempotent()).isTrue();
            verifyNoInteractions(invoiceRepository, riskEngine, financialMetricsService, xmlStorageService);
        }

        @Test
        @DisplayName("Should release idempotency and throw error on duplicate invoice")
        void shouldHandleDuplicateInvoice() {
            UUID invoiceId = UUID.randomUUID();
            Invoice existingInvoice = TestEntityHelper.withId(Invoice.of(XML, TENANT_ID, CHAVE_NFE), invoiceId);
            RiskAnalysis existingAnalysis = createPersistedAnalysis(UUID.randomUUID());

            when(idempotencyService.checkOrReverse(TENANT_ID, IDEM_KEY)).thenReturn(null);
            when(invoiceRepository.findByTenantIdAndNfeKey(TENANT_ID, CHAVE_NFE)).thenReturn(Optional.of(existingInvoice));
            when(riskRepository.findTopByTenantIdAndInvoiceIdOrderByCreatedAtDesc(TENANT_ID, invoiceId)).thenReturn(Optional.of(existingAnalysis));

            assertThatThrownBy(() -> service.analyze(createRequest(), TENANT_ID))
                    .isInstanceOf(DuplicateInvoiceException.class);

            verify(idempotencyService).release(TENANT_ID, IDEM_KEY);
        }

        @Test
        @DisplayName("Should release idempotency when parsing fails")
        void shouldHandleInvalidNFe() {
            AnalysisRequest request = new AnalysisRequest("invalid-xml", IDEM_KEY, new BigDecimal("1000"), 2.0);
            when(idempotencyService.checkOrReverse(TENANT_ID, IDEM_KEY)).thenReturn(null);
            when(nfeParser.parse("invalid-xml")).thenThrow(new InvalidNFeException("XML inválido"));

            assertThatThrownBy(() -> service.analyze(request, TENANT_ID))
                    .isInstanceOf(InvalidNFeException.class);

            verify(idempotencyService).release(TENANT_ID, IDEM_KEY);
        }

        @Test
        @DisplayName("Should reject requested advance above invoice face value")
        void shouldRejectAdvanceAboveFaceValue() {
            AnalysisRequest request = new AnalysisRequest(XML, IDEM_KEY, new BigDecimal("10000.01"), 2.0);
            when(idempotencyService.checkOrReverse(TENANT_ID, IDEM_KEY)).thenReturn(null);

            assertThatThrownBy(() -> service.analyze(request, TENANT_ID))
                    .isInstanceOf(InvalidFinancialParametersException.class)
                    .hasMessage("Valor de antecipação não pode ser maior que o valor total da NF-e");

            verify(idempotencyService).release(TENANT_ID, IDEM_KEY);
            verifyNoInteractions(sefazClient, riskEngine, financialMetricsService, invoiceRepository);
        }

        @Test
        @DisplayName("Should map a concurrent unique constraint violation to duplicate without storing XML")
        void shouldHandleConcurrentDuplicateWithoutOrphanFile() {
            when(idempotencyService.checkOrReverse(TENANT_ID, IDEM_KEY)).thenReturn(null);
            when(invoiceRepository.findByTenantIdAndNfeKey(TENANT_ID, CHAVE_NFE)).thenReturn(Optional.empty());
            when(sefazClient.consultStatus(CHAVE_NFE)).thenReturn(
                    SefazClient.ConsultationResult.available(InvoiceStatus.AUTHORIZED, "SEFAZ", "Autorizada"));
            when(companyService.resolveCompany(any(), eq(TENANT_ID))).thenReturn(mock(Company.class));
            when(riskEngine.score(any())).thenReturn(new RiskEngineService.EngineResult(
                    0.25, List.of(), "model-v1", false));
            when(financialMetricsService.calculate(any(), any(), anyDouble(), anyDouble())).thenReturn(createMetrics());
            when(invoiceRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("unique"));

            assertThatThrownBy(() -> service.analyze(createRequest(), TENANT_ID))
                    .isInstanceOf(DuplicateInvoiceException.class);

            verifyNoInteractions(xmlStorageService);
            verify(idempotencyService).release(TENANT_ID, IDEM_KEY);
        }

        @Test
        @DisplayName("Should mark analysis as partial when SEFAZ integration is unavailable")
        void shouldMarkPartialWhenSefazUnavailable() {
            UUID invoiceId = UUID.randomUUID();
            UUID analysisId = UUID.randomUUID();
            AnalysisRequest request = createRequest();
            RuleResult engineFactor = RuleResult.of("sefaz", "DOCUMENT", 0.4, 0.4, "INCREASE", "unavailable", "FALLBACK");

            when(idempotencyService.checkOrReverse(TENANT_ID, IDEM_KEY)).thenReturn(null);
            when(invoiceRepository.findByTenantIdAndNfeKey(TENANT_ID, CHAVE_NFE)).thenReturn(Optional.empty());
            when(sefazClient.consultStatus(CHAVE_NFE)).thenReturn(
                    SefazClient.ConsultationResult.unavailable("SEFAZ", "Fonte indisponível")
            );
            when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(inv -> TestEntityHelper.withId(inv.getArgument(0), invoiceId));
            when(companyService.resolveCompany(any(), eq(TENANT_ID))).thenReturn(mock(Company.class));
            when(riskEngine.score(any(ScoringContext.class))).thenReturn(new RiskEngineService.EngineResult(
                    0.4, List.of(engineFactor), "model-v1", true));
            when(financialMetricsService.calculate(any(), any(), anyDouble(), anyDouble())).thenReturn(createMetrics());
            when(riskRepository.save(any(RiskAnalysis.class))).thenAnswer(inv -> {
                RiskAnalysis analysis = inv.getArgument(0);
                TestEntityHelper.withId(analysis, analysisId);
                TestEntityHelper.withCreatedAt(analysis, Instant.now());
                return analysis;
            });

            service.analyze(request, TENANT_ID);

            ArgumentCaptor<ScoringContext> contextCaptor = ArgumentCaptor.forClass(ScoringContext.class);
            verify(riskEngine).score(contextCaptor.capture());
            assertThat(contextCaptor.getValue().sefazStatus()).isEqualTo(com.cypher.analysis.engine.SefazStatus.ERROR);
            assertThat(contextCaptor.getValue().hasUnavailableSource()).isTrue();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {
        @Test
        @DisplayName("Should map persisted analysis to response")
        void shouldMapToResponse() {
            UUID analysisId = UUID.randomUUID();
            RiskAnalysis existing = createPersistedAnalysis(analysisId);
            when(riskRepository.findByIdAndTenantId(analysisId, TENANT_ID)).thenReturn(Optional.of(existing));

            Optional<AnalysisResponse> response = service.findById(analysisId, TENANT_ID);

            assertThat(response).isPresent();
            assertThat(response.get().analysisId()).isEqualTo(analysisId);
        }

        @Test
        @DisplayName("Should not return analysis from another tenant")
        void shouldNotReturnAnalysisFromAnotherTenant() {
            UUID analysisId = UUID.randomUUID();
            UUID otherTenant = UUID.randomUUID();
            when(riskRepository.findByIdAndTenantId(analysisId, otherTenant)).thenReturn(Optional.empty());

            Optional<AnalysisResponse> response = service.findById(analysisId, otherTenant);

            assertThat(response).isEmpty();
            verify(riskRepository).findByIdAndTenantId(analysisId, otherTenant);
            verify(riskRepository, never()).findById(analysisId);
        }
    }

    // Helpers
    private AnalysisRequest createRequest() {
        return new AnalysisRequest(XML, IDEM_KEY, new BigDecimal("8500.00"), 3.2);
    }

    private FinancialMetrics createMetrics() {
        return FinancialMetrics.calculate(new BigDecimal("10000.00"), new BigDecimal("8500.00"), 3.2, 0.25);
    }

    private RiskAnalysis createPersistedAnalysis(UUID analysisId) {
        Invoice invoice = TestEntityHelper.withId(Invoice.of(XML, TENANT_ID, CHAVE_NFE));
        RiskAnalysis analysis = RiskAnalysis.of(
                invoice, TENANT_ID, 0.25, "model-v1",
                List.of(RiskFactor.from(RuleResult.of("r1", "c1", 0.1, 0.1, "I", "m", "s"))),
                createMetrics(), false
        );
        return TestEntityHelper.withCreatedAt(TestEntityHelper.withId(analysis, analysisId));
    }
}
