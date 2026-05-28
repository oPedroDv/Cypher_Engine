package com.cypher.infrastructure.http;

import com.cypher.company.service.FederalRevenueClient;
import org.springframework.stereotype.Component;

@Component
public class FederalRevenueHttpClient implements FederalRevenueClient {

    @Override
    public CnpjData query(String cnpj) {
        return CnpjData.unknown(cnpj);
    }
}
