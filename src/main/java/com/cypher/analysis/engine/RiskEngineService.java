package com.cypher.analysis.engine;

import com.cypher.analysis.engine.rules.RiskRule;
import com.cypher.analysis.engine.rules.RuleResult;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RiskEngineService {

    private static final logger log = LoggerFactory.getLogger(RiskEngineService.class);

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

        double finalScore = computeWeightedScore(results);
        boolean hasAnyFallback = results.stream().anyMatch(RuleResult::isFallBack);
        log.debug("Score final: {}. Fallback: {}. Regras executadas: {}",
                finalScore, hasAnyFallback, results.size());
        return new EngineResult(
                finalScore,
                results,
                registry.getModelVersion(),
                hasAnyFallback || context.hasUnavailableSource()
        );
    }

    private RuleResult executableRule(RiskRule rule, ScoringContext context) {
        try {
            RuleResult result = rule.evaluate(context);
            log.trace("Regra [{}] score={} contributio={}",
                    rule.getName(), result.score(), result.contribution());
            return result;
        } catch (Exception e) {
            log.warn("Regra [{}] falhou com exceção. Aplicanda fallback. Erro: {}",
                    rule.getName(), e.getMessage());
            return RuleResult.fallback(rule.getName(), "ERROR", 0.0);
        }
    }

    private double computeWeightedScore(List<RuleResult> results) {
        double totalContribution = results.stream()
                .mapToDouble(RuleResult::contribution)
                .sum();
        return Math.min(1.0, Math.max(0.0, totalContribution));
    }
    public record EngineResult(
            double score,
            List<RuleResult> factors,
            String modelVersion,
            boolean dataPartial
    ){}
}