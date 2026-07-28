package com.cypher.shared.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends CypherException {

    private final String idempotencyKey;

    public IdempotencyConflictException(String idempotencyKey) {
        this(idempotencyKey, "Análise em andamento para esta chave. Tente novamente em instantes.");
    }


    public IdempotencyConflictException(String idempotencyKey, String message) {
        super(
                message,
                HttpStatus.CONFLICT,
                "IDEMPOTENCY_CONFLICT"
        );
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
