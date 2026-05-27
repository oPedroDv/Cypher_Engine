package com.cypher.analysis.engine;

import com.cypher.analysis.domain.InvoiceStatus;

public enum SefazStatus {

    AUTHORIZED,
    PENDING,
    CANCELLED,
    DENIED,
    ERROR,
    UNAVAILABLE;

    public static SefazStatus from(InvoiceStatus invoiceStatus) {
        if (invoiceStatus == null) return UNAVAILABLE;
        return switch (invoiceStatus) {
            case AUTHORIZED -> AUTHORIZED;
            case CANCELLED  -> CANCELLED;
            case DENIED     -> DENIED;
            case PENDING    -> PENDING;
            case ERROR      -> ERROR;
        };
    }

    public boolean isApta() {
        return this == AUTHORIZED;
    }

    public boolean bloqueiaAntecipacao() {
        return this == CANCELLED || this == DENIED;
    }
}