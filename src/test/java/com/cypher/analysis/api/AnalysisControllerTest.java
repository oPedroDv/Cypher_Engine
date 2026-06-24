package com.cypher.analysis.api;

import com.cypher.analysis.api.dto.AnalysisRequest;
import com.cypher.analysis.api.dto.AnalysisResponse;
import com.cypher.analysis.domain.FinancialMetrics;
import com.cypher.analysis.domain.Invoice;
import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.engine.rules.RuleResult;
import com.cypher.analysis.service.AnalysisService;
import com.cypher.infrastructure.persistence.TenantContext;
import com.cypher.shared.exception.DuplicateInvoiceException;
import com.cypher.shared.web.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalysisControllerTest {

    private static final String XML = "<nfe><chNFe>12345678901234567890123456789012345678901234</chNFe></nfe>";
    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private AnalysisService service;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AnalysisController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void createAnalysisReturnsCreatedAndDelegatesValidatedRequest() throws Exception {
        UUID analysisId = UUID.randomUUID();
        AnalysisRequest request = new AnalysisRequest(XML, "idem-1", new BigDecimal("8500.00"), 3.2);
        when(service.analyze(any(AnalysisRequest.class), any())).thenReturn(response(analysisId, UUID.randomUUID(), false));

        mockMvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.analysisId").value(analysisId.toString()))
                .andExpect(jsonPath("$.idempotent").value(false))
                .andExpect(jsonPath("$.score").value(0.25))
                .andExpect(jsonPath("$.financial.requestedAdvanceValue").value(8500.00));

        ArgumentCaptor<AnalysisRequest> requestCaptor = ArgumentCaptor.forClass(AnalysisRequest.class);
        verify(service).analyze(requestCaptor.capture(), any());
        assertThat(requestCaptor.getValue().xmlBase64()).isEqualTo(XML);
        assertThat(requestCaptor.getValue().idempotencyKey()).isEqualTo("idem-1");
        assertThat(requestCaptor.getValue().requestedAdvanceValue()).isEqualByComparingTo("8500.00");
    }

    @Test
    void createAnalysisRejectsBlankXmlBeforeCallingService() throws Exception {
        AnalysisRequest request = new AnalysisRequest(" ", "idem-1", new BigDecimal("8500.00"), 3.2);

        mockMvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0]").value("xmlBase64: xmlBase64 é obrigatório"));

        verifyNoInteractions(service);
    }

    @Test
    void createAnalysisRejectsAdvanceWithMoreThanTwoDecimalPlaces() throws Exception {
        AnalysisRequest request = new AnalysisRequest(XML, "idem-1", new BigDecimal("799.699"), 3.2);

        mockMvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error_code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0]").value(
                        "requestedAdvanceValue: requestedAdvanceValue deve ter no máximo duas casas decimais"));

        verifyNoInteractions(service);
    }

    @Test
    void createAnalysisMapsDuplicateInvoiceToConflictResponse() throws Exception {
        AnalysisRequest request = new AnalysisRequest(XML, "idem-1", new BigDecimal("8500.00"), 3.2);
        when(service.analyze(any(AnalysisRequest.class), any()))
                .thenThrow(new DuplicateInvoiceException("chave-duplicada", "analysis-123"));

        mockMvc.perform(post("/api/v1/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("DUPLICATE_INVOICE"))
                .andExpect(jsonPath("$.existing_analysis_id").value("analysis-123"))
                .andExpect(jsonPath("$.path").value("/api/v1/analyses"));
    }

    @Test
    void findByIdReturnsExistingAnalysis() throws Exception {
        UUID analysisId = UUID.randomUUID();
        when(service.findById(analysisId, TENANT_ID)).thenReturn(Optional.of(response(analysisId, UUID.randomUUID(), true)));

        mockMvc.perform(get("/api/v1/analyses/{id}", analysisId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysisId").value(analysisId.toString()))
                .andExpect(jsonPath("$.idempotent").value(true));

        verify(service).findById(analysisId, TENANT_ID);
    }

    @Test
    void findByIdReturnsNotFoundWhenAnalysisDoesNotExist() throws Exception {
        UUID analysisId = UUID.randomUUID();
        when(service.findById(analysisId, TENANT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/analyses/{id}", analysisId))
                .andExpect(status().isNotFound());

        verify(service).findById(analysisId, TENANT_ID);
        verify(service, never()).analyze(any(), any());
    }

    private static AnalysisResponse response(UUID analysisId, UUID invoiceId, boolean idempotent) {
        Invoice invoice = Invoice.of(XML, TENANT_ID, "12345678901234567890123456789012345678901234");
        ReflectionTestUtils.setField(invoice, "id", invoiceId);
        RiskAnalysis analysis = RiskAnalysis.of(
                invoice,
                TENANT_ID,
                0.25,
                "model-v1",
                List.of(com.cypher.analysis.domain.RiskFactor.from(
                        RuleResult.of("sefaz", "DOCUMENT", 0.25, 0.4, "INCREASE", "ok", "SEFAZ")
                )),
                FinancialMetrics.calculate(new BigDecimal("10000.00"), new BigDecimal("8500.00"), 3.2, 0.25),
                false
        );
        ReflectionTestUtils.setField(analysis, "id", analysisId);
        ReflectionTestUtils.setField(analysis, "createdAt", Instant.parse("2026-05-27T12:00:00Z"));
        return AnalysisResponse.from(analysis, idempotent);
    }
}
