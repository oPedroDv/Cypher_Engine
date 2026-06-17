package com.cypher.analysis.repository;

import java.math.BigDecimal;

public interface InvoiceHistoryStats {

    Number getIssuerTotal();

    Number getPayerTotal();

    Number getPairTotal();

    BigDecimal getIssuerAvgValue();

    default int issuerTotalAsInt() {
        return getIssuerTotal() == null ? 0 : Math.toIntExact(getIssuerTotal().longValue());
    }

    default int payerTotalAsInt() {
        return getPayerTotal() == null ? 0 : Math.toIntExact(getPayerTotal().longValue());
    }

    default int pairTotalAsInt() {
        return getPairTotal() == null ? 0 : Math.toIntExact(getPairTotal().longValue());
    }

    default BigDecimal issuerAvgValue() {
        BigDecimal value = getIssuerAvgValue();
        return value != null && value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
    }
}
