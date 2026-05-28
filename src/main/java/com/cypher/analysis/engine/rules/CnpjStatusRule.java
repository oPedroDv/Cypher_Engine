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

        double issuerScore = scoreForStatus(issuerStatus);
        double payerScore  = scoreForStatus(payerStatus);

        if (issuerScore >= payerScore) {
            return buildResult(issuerScore, "cedente", issuerStatus);
        } else {
            return buildResult(payerScore, "sacado", payerStatus);
        }
    }

    private RuleResult buildResult(double score, String party, CnpjStatus status) {
        String direction = score > 0.0 ? "INCREASE" : "DECREASE";
        return RuleResult.of(getName(), "cnpj_validation", score, WEIGHT, direction,
                buildExplanation(party, status), "RECEITA_FEDERAL");
    }

    private String buildExplanation(String party, CnpjStatus status) {
        return switch (status) {
            case ACTIVE    -> "CNPJ do %s ativo e regular na Receita Federal.".formatted(party);
            case SUSPENDED -> "CNPJ do %s suspenso — pendências fiscais.".formatted(party);
            case UNFIT     -> "CNPJ do %s inapto — irregularidades graves.".formatted(party);
            case CLOSED    -> "CNPJ do %s baixado — empresa encerrada. Antecipação inviável.".formatted(party);
            case NULLIFIED -> "CNPJ do %s nulo ou cancelado — possível fraude.".formatted(party);
            case UNKNOWN   -> "Situação cadastral do %s não verificada — score conservador aplicado.".formatted(party);
        };
    }

    private double scoreForStatus(CnpjStatus status) {
        return switch (status) {
            case ACTIVE    -> 0.00;
            case SUSPENDED -> 0.35;
            case UNFIT     -> 0.70;
            case CLOSED    -> 1.00;
            case NULLIFIED -> 1.00;
            case UNKNOWN   -> 0.50;
        };
    }

    @Override
    public String getName() { return "cnpj_status"; }

    @Override
    public double getWeight() { return WEIGHT; }
}
