package com.cypher.company.domain;

public enum CnpjStatus {

    ACTIVE,
    SUSPENDED,
    UNFIT,
    CLOSED,
    NULLIFIED,
    UNKNOWN;

    public boolean isFit() {
        return this == ACTIVE;
    }

    public boolean isCritical() {
        return this == CLOSED || this == NULLIFIED;
    }
}
