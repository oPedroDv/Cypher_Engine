package com.cypher.analysis.engine;

import com.cypher.analysis.engine.rules.RiskRule;
import com.cypher.analysis.engine.rules.RuleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

        double finalScore       = computeWeightedScore(results);
        boolean hasAnyFallback  = results.stream().anyMatch(RuleResult::isFallback);

        log.debug("Score final: {}. Fallback: {}. Regras executadas: {}",
                finalScore, hasAnyFallback, results.size());

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
        return Math.min(1.0, Math.max(0.0, total));
    }

    public record EngineResult(
            double score,
            List<RuleResult> factors,
            String modelVersion,
            boolean dataPartial
    ) {}
}