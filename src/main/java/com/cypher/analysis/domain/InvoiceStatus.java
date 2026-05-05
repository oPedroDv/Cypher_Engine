package com.cypher.analysis.domain;

public enum InvoiceStatus {
    PENDING,
    AUTHORIZED,
    CANCELLED,
    DENIED,
    ERROR;

    public static InvoiceStatus fromSefazCode(String code) {
        if (code == null) return ERROR;
        return switch (code.trim()) {
            case "100" -> AUTHORIZED;
            case "101", "151", "155" -> CANCELLED;
            case "110", "301", "302" -> DENIED;
            default -> PENDING;
        };
    }

    public boolean isOperational(){
        return this == AUTHORIZED;
    }

    public boolean blockAnticipation(){
        return this == CANCELLED || this == DENIED;
    }
}