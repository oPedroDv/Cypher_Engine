package com.cypher.company.service;

import com.cypher.company.domain.CnpjStatus;

public interface FederalRevenueClient {

    CnpjData query(String cnpj);

    record CnpjData(
            String cnpj,
            String legalName,
            String tradeName,
            CnpjStatus status
    ) {
        public static CnpjData unknown(String cnpj) {
            return new CnpjData(cnpj, "EMPRESA DESCONHECIDA", null, CnpjStatus.UNKNOWN);
        }
    }
}
