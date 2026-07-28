package com.cypher.testutil;

import com.cypher.analysis.domain.NFeData;
import com.cypher.analysis.engine.ScoringContext;
import com.cypher.company.domain.CnpjStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ScoringContextFixture {

    public static final String ACCESS_KEY = "35240112345678000190550010000000011000000017";

    private ScoringContextFixture() {}

    public static NFeData nfe(BigDecimal totalAmount, LocalDate dueDate) {
        return NFeData.builder()
                .accessKey(ACCESS_KEY)
                .number("1")
                .series("1")
                .issuerCnpj("12345678000190")
                .issuerLegalName("CEDENTE LTDA")
                .recipientCnpj("98765432000110")
                .recipientLegalName("SACADO SA")
                .totalAmount(totalAmount)
                .issueDate(LocalDate.now())
                .dueDate(dueDate)
                .build();
    }

    public static ScoringContext.ScoringContextBuilder base() {
        return ScoringContext.builder()
                .nfeData(nfe(new BigDecimal("10000.00"), LocalDate.now().plusDays(30)))
                .issuerCnpjStatus(CnpjStatus.ACTIVE)
                .payerCnpjStatus(CnpjStatus.ACTIVE);
    }
}
