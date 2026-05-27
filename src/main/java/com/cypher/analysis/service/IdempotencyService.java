package com.cypher.analysis.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    private static final String PREFIX         = "cypher:idempotency:";
    private static final String PROCESSING     = "PROCESSING";
    private static final Duration RESULT_TTL   = Duration.ofHours(24);
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(2);

    private final StringRedisTemplate redis;

    public IdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String checkOrReverse(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return null;

        String redisKey = PREFIX + idempotencyKey;
        String existing = redis.opsForValue().get(redisKey);

        if (existing != null) {
            if (PROCESSING.equals(existing)) {
                log.warn("Chave de idempotência em processamento: {}", idempotencyKey);
                throw new IdempotencyConflictException(
                        "Análise em andamento para esta chave. Tente novamente em instantes.",
                        idempotencyKey, null);
            }
            log.debug("Hit idempotente: {} → analysisId={}", idempotencyKey, existing);
            return existing;
        }

        Boolean reserved = redis.opsForValue().setIfAbsent(redisKey, PROCESSING, PROCESSING_TTL);
        if (Boolean.FALSE.equals(reserved)) {
            throw new IdempotencyConflictException(
                    "Análise em andamento para esta chave. Tente novamente em instantes.",
                    idempotencyKey, null);
        }

        log.debug("Chave de idempotência reservada: {}", idempotencyKey);
        return null;
    }

    public void confirm(String idempotencyKey, String analysisId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;

        String redisKey = PREFIX + idempotencyKey;
        redis.opsForValue().set(redisKey, analysisId, RESULT_TTL);
        log.debug("Chave de idempotência confirmada: {} → analysisId={}", idempotencyKey, analysisId);
    }

    public void release(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;

        String redisKey = PREFIX + idempotencyKey;
        redis.delete(redisKey);
        log.debug("Chave de idempotência liberada após erro: {}", idempotencyKey);
    }

    public static class IdempotencyConflictException extends RuntimeException {

        private final String idempotencyKey;
        private final String existingAnalysisId;

        public IdempotencyConflictException(String message, String key, String existingId) {
            super(message);
            this.idempotencyKey      = key;
            this.existingAnalysisId  = existingId;
        }

        public String getIdempotencyKey()     { return idempotencyKey; }
        public String getExistingAnalysisId() { return existingAnalysisId; }
    }
}