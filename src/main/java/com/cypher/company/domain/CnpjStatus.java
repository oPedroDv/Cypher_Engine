package com.cypher.company.domain;

public enum CnpjStatus {

    ATIVA,
    SUSPENSA,
    INAPTA,
    BAIXADA,
    NULA,
    DESCONHECIDO;

    public boolean isApta() {
        return this == ATIVA;
    }

    public boolean isCritico() {
        return this == BAIXADA || this == NULA;
    }
}