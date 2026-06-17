package com.cypher.analysis.engine;

import com.cypher.analysis.domain.InvoiceStatus;

public enum SefazStatus {

    AUTHORIZED,
    PENDING,
    CANCELLED,
    DENIED,
    ERROR,
    UNAVAILABLE,
    NOT_CONFIGURED;

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

    public static SefazStatus from(InvoiceStatus invoiceStatus, boolean notConfigured) {
        return notConfigured ? NOT_CONFIGURED : from(invoiceStatus);
    }

    public boolean isFit() {
        return this == AUTHORIZED;
    }

    public boolean blocksAdvance() {
        return this == CANCELLED || this == DENIED;
    }
}
