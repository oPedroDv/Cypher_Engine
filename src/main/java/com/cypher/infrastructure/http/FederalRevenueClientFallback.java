package com.cypher.infrastructure.http;

import com.cypher.company.service.FederalRevenueClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "receita-federal.enabled", havingValue = "false")
public class FederalRevenueClientFallback implements FederalRevenueClient {
    @Override
    public CnpjData query(String cnpj) {
        log.warn("Receita Federal desabilitada — CNPJ status retornará UNKNOWN");
        return CnpjData.unknown(cnpj);
    }
}
