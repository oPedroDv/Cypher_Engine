package com.cypher.analysis.engine;

import com.cypher.analysis.domain.InvoiceStatus;
import com.cypher.analysis.domain.NFeData;
import com.cypher.analysis.engine.rules.RiskRule;
import com.cypher.analysis.engine.rules.RuleResult;
import com.cypher.company.domain.CnpjStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskEngineServiceTest {

    @Mock
    private RuleRegistry registry;

    @Mock
    private RiskRule lowRiskRule;

    @Mock
    private RiskRule highRiskRule;

    private RiskEngineService service;

    @BeforeEach
    void setUp() {
        service = new RiskEngineService(registry);
        when(registry.getModelVersion()).thenReturn("model-test");
    }

    @Test
    void scoreComputesWeightedScoreFromRuleContributions() {
        ScoringContext context = context(false);
        when(registry.getActiveRules()).thenReturn(List.of(lowRiskRule, highRiskRule));
        when(lowRiskRule.evaluate(context)).thenReturn(
                RuleResult.of("sefaz_status", "nfe_validation", 0.2, 0.25, "INCREASE", "pending", "SEFAZ")
        );
        when(highRiskRule.evaluate(context)).thenReturn(
                RuleResult.of("payer_history", "behavioral", 0.8, 0.50, "INCREASE", "defaults", "INTERNAL_HISTORY")
        );

        RiskEngineService.EngineResult result = service.score(context);

        assertThat(result.score()).isEqualTo(0.45);
        assertThat(result.modelVersion()).isEqualTo("model-test");
        assertThat(result.dataPartial()).isFalse();
        assertThat(result.factors()).extracting(RuleResult::ruleName)
                .containsExactly("sefaz_status", "payer_history");
        verify(lowRiskRule).evaluate(context);
        verify(highRiskRule).evaluate(context);
    }

    @Test
    void scoreClampsTotalContributionToOne() {
        ScoringContext context = context(false);
        when(registry.getActiveRules()).thenReturn(List.of(lowRiskRule, highRiskRule));
        when(lowRiskRule.evaluate(context)).thenReturn(
                new RuleResult("rule-a", "stress", 1.0, 0.8, 0.8, "INCREASE", "a", "TEST")
        );
        when(highRiskRule.evaluate(context)).thenReturn(
                new RuleResult("rule-b", "stress", 1.0, 0.7, 0.7, "INCREASE", "b", "TEST")
        );

        RiskEngineService.EngineResult result = service.score(context);

        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.dataPartial()).isFalse();
    }

    @Test
    void scoreAppliesFallbackAndMarksDataPartialWhenRuleThrows() {
        ScoringContext context = context(false);
        when(registry.getActiveRules()).thenReturn(List.of(lowRiskRule));
        when(lowRiskRule.getName()).thenReturn("sefaz_status");
        when(lowRiskRule.getWeight()).thenReturn(0.25);
        when(lowRiskRule.evaluate(context)).thenThrow(new IllegalStateException("source down"));

        RiskEngineService.EngineResult result = service.score(context);

        assertThat(result.score()).isEqualTo(0.35);
        assertThat(result.dataPartial()).isTrue();
        assertThat(result.factors()).singleElement().satisfies(factor -> {
            assertThat(factor.ruleName()).isEqualTo("sefaz_status");
            assertThat(factor.category()).isEqualTo("ERROR");
            assertThat(factor.isFallback()).isTrue();
            assertThat(factor.dataSource()).isEqualTo("FALLBACK");
        });
    }

    @Test
    void scoreMarksDataPartialWhenContextHasUnavailableSourceEvenWithoutFallbacks() {
        ScoringContext context = context(true);
        when(registry.getActiveRules()).thenReturn(List.of(lowRiskRule));
        when(lowRiskRule.evaluate(context)).thenReturn(
                RuleResult.of("cnpj_status", "cnpj_validation", 0.0, 0.10, "DECREASE", "regular", "RECEITA_FEDERAL")
        );

        RiskEngineService.EngineResult result = service.score(context);

        assertThat(result.score()).isEqualTo(0.35);
        assertThat(result.dataPartial()).isTrue();
    }

    @Test
    void scoreEscalatesCancelledSefazToCriticalEvenWhenWeightedRulesAreLow() {
        ScoringContext context = context(false, SefazStatus.CANCELLED, CnpjStatus.ACTIVE, CnpjStatus.ACTIVE);
        when(registry.getActiveRules()).thenReturn(List.of(lowRiskRule));
        when(lowRiskRule.evaluate(context)).thenReturn(
                RuleResult.of("issuer_history", "behavioral", 0.1, 0.20, "DECREASE", "low", "INTERNAL_HISTORY")
        );

        RiskEngineService.EngineResult result = service.score(context);

        assertThat(result.score()).isEqualTo(0.95);
    }

    @Test
    void scoreEscalatesCriticalIssuerCnpjEvenWhenWeightedRulesAreLow() {
        ScoringContext context = context(false, SefazStatus.AUTHORIZED, CnpjStatus.CLOSED, CnpjStatus.ACTIVE);
        when(registry.getActiveRules()).thenReturn(List.of(lowRiskRule));
        when(lowRiskRule.evaluate(context)).thenReturn(
                RuleResult.of("sefaz_status", "nfe_validation", 0.0, 0.25, "DECREASE", "ok", "SEFAZ")
        );

        RiskEngineService.EngineResult result = service.score(context);

        assertThat(result.score()).isEqualTo(0.90);
    }

    @Test
    void scoreEscalatesSevereHistoricalDefaultRates() {
        ScoringContext context = ScoringContext.builder()
                .nfeData(NFeData.builder()
                        .accessKey("12345678901234567890123456789012345678901234")
                        .totalAmount(new BigDecimal("10000.00"))
                        .dueDate(LocalDate.now().plusDays(30))
                        .status(InvoiceStatus.AUTHORIZED)
                        .build())
                .sefazStatus(SefazStatus.AUTHORIZED)
                .issuerCnpjStatus(CnpjStatus.ACTIVE)
                .payerCnpjStatus(CnpjStatus.ACTIVE)
                .issuerTotalInvoices(20)
                .issuerDefaultCount(7)
                .payerTotalInvoices(20)
                .payerDefaultCount(6)
                .pairTotalInvoices(10)
                .pairDefaultCount(2)
                .build();

        when(registry.getActiveRules()).thenReturn(List.of(lowRiskRule));
        when(lowRiskRule.evaluate(context)).thenReturn(
                RuleResult.of("sefaz_status", "nfe_validation", 0.0, 0.25, "DECREASE", "ok", "SEFAZ")
        );

        RiskEngineService.EngineResult result = service.score(context);

        assertThat(result.score()).isEqualTo(0.90);
    }

    private static ScoringContext context(boolean hasUnavailableSource) {
        return context(hasUnavailableSource, SefazStatus.AUTHORIZED, CnpjStatus.ACTIVE, CnpjStatus.ACTIVE);
    }

    private static ScoringContext context(
            boolean hasUnavailableSource,
            SefazStatus sefazStatus,
            CnpjStatus issuerStatus,
            CnpjStatus payerStatus
    ) {
        return ScoringContext.builder()
                .nfeData(NFeData.builder()
                        .accessKey("12345678901234567890123456789012345678901234")
                        .totalAmount(new BigDecimal("10000.00"))
                        .dueDate(LocalDate.now().plusDays(30))
                        .status(InvoiceStatus.AUTHORIZED)
                        .build())
                .sefazStatus(sefazStatus)
                .issuerCnpjStatus(issuerStatus)
                .payerCnpjStatus(payerStatus)
                .hasUnavailableSource(hasUnavailableSource)
                .build();
    }
}
