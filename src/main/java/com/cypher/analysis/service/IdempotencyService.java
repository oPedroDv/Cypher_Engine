package com.cypher.analysis.service;

import com.cypher.shared.exception.IdempotencyConflictException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class IdempotencyService {

    private static final String PREFIX           = "cypher:idempotency:";
    private static final String PROCESSING       = "PROCESSING";
    private static final Duration RESULT_TTL     = Duration.ofHours(24);
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
                throw new IdempotencyConflictException(idempotencyKey);
            }
            log.debug("Hit idempotente: {} → analysisId={}", idempotencyKey, existing);
            return existing;
        }

        Boolean reserved = redis.opsForValue().setIfAbsent(redisKey, PROCESSING, PROCESSING_TTL);
        if (Boolean.FALSE.equals(reserved)) {
            throw new IdempotencyConflictException(idempotencyKey);
        }

        log.debug("Chave de idempotência reservada: {}", idempotencyKey);
        return null;
    }

    public void confirm(String idempotencyKey, String analysisId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;

        redis.opsForValue().set(PREFIX + idempotencyKey, analysisId, RESULT_TTL);
        log.debug("Chave de idempotência confirmada: {} → analysisId={}", idempotencyKey, analysisId);
    }

    public void release(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;

        redis.delete(PREFIX + idempotencyKey);
        log.debug("Chave de idempotência liberada após erro: {}", idempotencyKey);
    }
}