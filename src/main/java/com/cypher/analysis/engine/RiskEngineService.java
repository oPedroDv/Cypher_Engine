package com.cypher.analysis.engine;

import com.cypher.analysis.engine.rules.RiskRule;
import com.cypher.analysis.engine.rules.RuleResult;
import com.cypher.company.domain.CnpjStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class RiskEngineService {

    private static final Logger log = LoggerFactory.getLogger(RiskEngineService.class);

    private final RuleRegistry registry;
    private final RiskEngineConfig config;

    @Autowired
    public RiskEngineService(RuleRegistry registry, RiskEngineConfig config) {
        this.registry = registry;
        this.config = config;
    }


    public RiskEngineService(RuleRegistry registry) {
        this(registry, new RiskEngineConfig());
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
        } catch (RuntimeException e) {
            log.error("Regra [{}] falhou com exceção. Aplicando fallback. Erro: {}",
                    rule.getName(), e.getMessage(), e);
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
            score += config.getSourceUnavailablePenalty();
            score = Math.max(score, config.getSourceUnavailableFloor());
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
            return config.getExternalValidationUnknownFloor();
        }
        return config.getExternalValidationFloor();
    }

    private double sefazFloor(SefazStatus status) {
        return switch (status) {
            case CANCELLED, DENIED -> config.getSefazBlockedFloor();
            case PENDING -> config.getSefazPendingFloor();
            case ERROR, UNAVAILABLE -> config.getSefazUnavailableFloor();
            case AUTHORIZED, NOT_CONFIGURED -> 0.0;
        };
    }

    private double cnpjFloor(CnpjStatus status, boolean issuer) {
        return switch (status) {
            case CLOSED, NULLIFIED -> issuer ? config.getIssuerClosedFloor() : config.getPayerClosedFloor();
            case UNFIT -> issuer ? config.getIssuerUnfitFloor() : config.getPayerUnfitFloor();
            case SUSPENDED -> issuer ? config.getIssuerSuspendedFloor() : config.getPayerSuspendedFloor();
            case UNKNOWN -> 0.0;
            case ACTIVE -> 0.0;
        };
    }

    private double historicalFloor(ScoringContext context) {
        double floor = 0.0;

        floor = Math.max(floor, rateFloor(context.issuerDefaultRate(),
                config.getIssuerHighThreshold(), config.getIssuerCriticalThreshold(),
                config.getIssuerHighFloor(), config.getIssuerCriticalFloor()));
        floor = Math.max(floor, rateFloor(context.payerDefaultRate(),
                config.getPayerHighThreshold(), config.getPayerCriticalThreshold(),
                config.getPayerHighFloor(), config.getPayerCriticalFloor()));
        floor = Math.max(floor, rateFloor(context.pairDefaultRate(),
                config.getPairHighThreshold(), config.getPairCriticalThreshold(),
                config.getPairHighFloor(), config.getPairCriticalFloor()));

        if (context.payerLatePaymentRate() >= config.getPayerLateThreshold()) {
            floor = Math.max(floor, config.getPayerLateFloor());
        }
        return floor;
    }

    private double rateFloor(double rate, double highThreshold, double criticalThreshold, double highFloor, double criticalFloor) {
        if (rate >= criticalThreshold) return criticalFloor;
        if (rate >= highThreshold) return highFloor;
        return 0.0;
    }

    private double maturityFloor(ScoringContext context) {
        if (context.nfeData().getDueDate() == null) return config.getMissingMaturityFloor();
        long daysUntilDue = ChronoUnit.DAYS.between(LocalDate.now(), context.nfeData().getDueDate());
        if (daysUntilDue < 0) return config.getOverdueMaturityFloor();
        if (daysUntilDue <= config.getNearMaturityDays()) return config.getNearMaturityFloor();
        return 0.0;
    }

    private double valueAnomalyFloor(ScoringContext context) {
        BigDecimal avgValue = context.issuerAvgValue();
        BigDecimal totalAmount = context.nfeData().getTotalAmount();
        if (avgValue == null || totalAmount == null || avgValue.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }

        double ratio = totalAmount.doubleValue() / avgValue.doubleValue();

        if (ratio > config.getValueAnomalyCriticalRatio()) return config.getValueAnomalyCriticalFloor();
        if (ratio > config.getValueAnomalyHighRatio()) return config.getValueAnomalyHighFloor();
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
