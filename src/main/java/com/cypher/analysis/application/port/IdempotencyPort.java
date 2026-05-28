package com.cypher.analysis.application.port;

public interface IdempotencyPort {

    String checkOrReverse(String idempotencyKey);

    void confirm(String idempotencyKey, String analysisId);

    void release(String idempotencyKey);
}
