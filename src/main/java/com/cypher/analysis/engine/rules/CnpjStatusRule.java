package com.cypher.analysis.engine.rules;

import com.cypher.analysis.engine.ScoringContext;
import com.cypher.company.domain.CnpjStatus;
import org.springframework.stereotype.Component;

@Component
public class CnpjStatusRule implements RiskRule {

    private static final double WEIGHT = 0.10;

    @Override
    public RuleResult evaluate(ScoringContext context) {
        CnpjStatus issuerStatus = context.issuerCnpjStatus();
        CnpjStatus payerStatus  = context.payerCnpjStatus();

        double issuerScore = scoreParaStatus(issuerStatus);
        double payerScore  = scoreParaStatus(payerStatus);

        if (issuerScore >= payerScore) {
            return buildResult(issuerScore, "cedente", issuerStatus);
        } else {
            return buildResult(payerScore, "sacado", payerStatus);
        }
    }

    private RuleResult buildResult(double score, String parte, CnpjStatus status) {
        String direction = score > 0.0 ? "INCREASE" : "DECREASE";
        return RuleResult.of(getName(), "cnpj_validation", score, WEIGHT, direction,
                buildExplanation(parte, status), "RECEITA_FEDERAL");
    }

    private String buildExplanation(String parte, CnpjStatus status) {
        return switch (status) {
            case ATIVA        -> "CNPJ do %s ativo e regular na Receita Federal.".formatted(parte);
            case SUSPENSA     -> "CNPJ do %s suspenso — pendências fiscais.".formatted(parte);
            case INAPTA       -> "CNPJ do %s inapto — irregularidades graves.".formatted(parte);
            case BAIXADA      -> "CNPJ do %s baixado — empresa encerrada. Antecipação inviável.".formatted(parte);
            case NULA         -> "CNPJ do %s nulo ou cancelado — possível fraude.".formatted(parte);
            case DESCONHECIDO -> "Situação cadastral do %s não verificada — score conservador aplicado.".formatted(parte);
        };
    }

    private double scoreParaStatus(CnpjStatus status) {
        return switch (status) {
            case ATIVA        -> 0.00;
            case SUSPENSA     -> 0.35;
            case INAPTA       -> 0.70;
            case BAIXADA      -> 1.00;
            case NULA         -> 1.00;
            case DESCONHECIDO -> 0.50;
        };
    }

    @Override
    public String getName() { return "cnpj_status"; }

    @Override
    public double getWeight() { return WEIGHT; }
}