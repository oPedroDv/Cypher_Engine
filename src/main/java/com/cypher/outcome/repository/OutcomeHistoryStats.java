package com.cypher.outcome.repository;

public record OutcomeHistoryStats(
        long issuerDefaults,
        long payerDefaults,
        long pairDefaults,
        long payerLatePayments
) {
    public int issuerDefaultsAsInt() {
        return Math.toIntExact(issuerDefaults);
    }

    public int payerDefaultsAsInt() {
        return Math.toIntExact(payerDefaults);
    }

    public int pairDefaultsAsInt() {
        return Math.toIntExact(pairDefaults);
    }

    public int payerLatePaymentsAsInt() {
        return Math.toIntExact(payerLatePayments);
    }
}
