package com.cypher.analysis.engine;

import com.cypher.analysis.engine.rules.RiskRule;
import com.cypher.analysis.engine.rules.RuleResult;
import com.cypher.company.domain.CnpjStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RiskEngineService {

    private static final Logger log = LoggerFactory.getLogger(RiskEngineService.class);

    private final RuleRegistry registry;

    public RiskEngineService(RuleRegistry registry) {
        this.registry = registry;
    }

    public EngineResult score(ScoringContext context) {
        log.debug("Iniciando scoring. Model: {}. SEFAZ: {}. CNPJ cedente: {}",
                registry.getModelVersion(),
                context.sefazStatus(),
                context.issuerCnpjStatus());

        List<RuleResult> results = registry.getActiveRules().stream()
                .map(rule -> executeRule(rule, context))
                .toList();

        double weightedScore = computeWeightedScore(results);
        boolean hasAnyFallback = results.stream().anyMatch(RuleResult::isFallback);
        double finalScore = applyEscalationFloor(weightedScore, context, hasAnyFallback);

        log.debug("Score final: {}. Score ponderado: {}. Fallback: {}. Regras executadas: {}",
                finalScore, weightedScore, hasAnyFallback, results.size());

        return new EngineResult(
                finalScore,
                results,
                registry.getModelVersion(),
                hasAnyFallback || context.hasUnavailableSource()
        );
    }

    private RuleResult executeRule(RiskRule rule, ScoringContext context) {
        try {
            RuleResult result = rule.evaluate(context);
            log.trace("Regra [{}] score={} contribution={}",
                    rule.getName(), result.score(), result.contribution());
            return result;
        } catch (Exception e) {
            log.warn("Regra [{}] falhou com exceção. Aplicando fallback. Erro: {}",
                    rule.getName(), e.getMessage());
            return RuleResult.fallback(rule.getName(), "ERROR", rule.getWeight());
        }
    }

    private double computeWeightedScore(List<RuleResult> results) {
        double total = results.stream()
                .mapToDouble(RuleResult::contribution)
                .sum();
        return clamp(total);
    }

    private double applyEscalationFloor(double weightedScore, ScoringContext context, boolean hasAnyFallback) {
        double floor = 0.0;

        floor = Math.max(floor, sefazFloor(context.sefazStatus()));
        floor = Math.max(floor, cnpjFloor(context.issuerCnpjStatus(), true));
        floor = Math.max(floor, cnpjFloor(context.payerCnpjStatus(), false));
        floor = Math.max(floor, historicalFloor(context));
        floor = Math.max(floor, maturityFloor(context));
        floor = Math.max(floor, valueAnomalyFloor(context));
        floor = Math.max(floor, externalValidationFloor(context));

        double score = Math.max(weightedScore, floor);
        if (shouldApplySourceUnavailablePenalty(context, hasAnyFallback)) {
            score += 0.10;
            score = Math.max(score, 0.35);
        }
        return clamp(score);
    }

    private boolean shouldApplySourceUnavailablePenalty(ScoringContext context, boolean hasAnyFallback) {
        return hasAnyFallback || (context.hasUnavailableSource() && context.sefazStatus() != SefazStatus.NOT_CONFIGURED);
    }

    private double externalValidationFloor(ScoringContext context) {
        if (context.sefazStatus() != SefazStatus.NOT_CONFIGURED) {
            return 0.0;
        }
        if (context.issuerCnpjStatus() == CnpjStatus.UNKNOWN || context.payerCnpjStatus() == CnpjStatus.UNKNOWN) {
            return 0.25;
        }
        return 0.20;
    }

    private double sefazFloor(SefazStatus status) {
        return switch (status) {
            case CANCELLED, DENIED -> 0.95;
            case PENDING -> 0.70;
            case ERROR, UNAVAILABLE -> 0.45;
            case AUTHORIZED, NOT_CONFIGURED -> 0.0;
        };
    }

    private double cnpjFloor(CnpjStatus status, boolean issuer) {
        return switch (status) {
            case CLOSED, NULLIFIED -> issuer ? 0.90 : 0.80;
            case UNFIT -> issuer ? 0.80 : 0.70;
            case SUSPENDED -> issuer ? 0.65 : 0.55;
            case UNKNOWN -> 0.0;
            case ACTIVE -> 0.0;
        };
    }

    private double historicalFloor(ScoringContext context) {
        double floor = 0.0;
        floor = Math.max(floor, rateFloor(context.issuerDefaultRate(), 0.15, 0.30, 0.70, 0.85));
        floor = Math.max(floor, rateFloor(context.payerDefaultRate(), 0.10, 0.25, 0.70, 0.90));
        floor = Math.max(floor, rateFloor(context.pairDefaultRate(), 0.05, 0.15, 0.65, 0.85));

        if (context.payerLatePaymentRate() >= 0.30) {
            floor = Math.max(floor, 0.60);
        }
        return floor;
    }

    private double rateFloor(double rate, double highThreshold, double criticalThreshold, double highFloor, double criticalFloor) {
        if (rate >= criticalThreshold) return criticalFloor;
        if (rate >= highThreshold) return highFloor;
        return 0.0;
    }

    private double maturityFloor(ScoringContext context) {
        if (context.nfeData().getDueDate() == null) return 0.40;
        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), context.nfeData().getDueDate());
        if (daysUntilDue < 0) return 0.75;
        if (daysUntilDue <= 3) return 0.60;
        return 0.0;
    }

    private double valueAnomalyFloor(ScoringContext context) {
        BigDecimal avgValue = context.issuerAvgValue();
        BigDecimal totalAmount = context.nfeData().getTotalAmount();
        if (avgValue == null || totalAmount == null || avgValue.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }

        double ratio = totalAmount.doubleValue() / avgValue.doubleValue();
        if (ratio > 8.0) return 0.75;
        if (ratio > 5.0) return 0.65;
        return 0.0;
    }

    private double clamp(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }

    public record EngineResult(
            double score,
            List<RuleResult> factors,
            String modelVersion,
            boolean dataPartial
    ) {}
}
