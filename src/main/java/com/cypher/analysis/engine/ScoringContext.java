package com.cypher.analysis.engine;

import com.cypher.analysis.domain.NFeData;
import com.cypher.company.domain.CnpjStatus;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ScoringContext(

        NFeData nfeData,

        SefazStatus sefazStatus,
        CnpjStatus issuerCnpjStatus,
        CnpjStatus payerCnpjStatus,

        int issuerTotalInvoices,
        int issuerDefaultCount,
        BigDecimal issuerAvgValue,

        int payerTotalInvoices,
        int payerLatePaymentCount,
        int payerDefaultCount,

        int pairTotalInvoices,
        int pairDefaultCount,

        BigDecimal requestedAdvanceValue,
        double requestedMonthlyRate,

        boolean hasUnavailableSource

) {

    public ScoringContext {
        if (nfeData == null) {
            throw new IllegalArgumentException("nfeData é obrigatório no ScoringContext");
        }

        sefazStatus = sefazStatus == null ? SefazStatus.UNAVAILABLE : sefazStatus;
        issuerCnpjStatus = issuerCnpjStatus == null ? CnpjStatus.UNKNOWN : issuerCnpjStatus;
        payerCnpjStatus  = payerCnpjStatus == null ? CnpjStatus.UNKNOWN : payerCnpjStatus;
    }

    public double issuerDefaultRate() {
        if (issuerTotalInvoices == 0) return 0.0;
        return (double) issuerDefaultCount / issuerTotalInvoices;
    }

    public double payerLatePaymentRate() {
        if (payerTotalInvoices == 0) return 0.0;
        return (double) payerLatePaymentCount / payerTotalInvoices;
    }

    public double payerDefaultRate() {
        if (payerTotalInvoices == 0) return 0.0;
        return (double) payerDefaultCount / payerTotalInvoices;
    }

    public double pairDefaultRate() {
        if (pairTotalInvoices == 0) return 0.0;
        return (double) pairDefaultCount / pairTotalInvoices;
    }
}
