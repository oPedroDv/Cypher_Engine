package com.cypher.analysis.service;

import com.cypher.shared.exception.IdempotencyConflictException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
public class IdempotencyService {

    private static final String PREFIX           = "cypher:idempotency:";
    private static final String PROCESSING       = "PROCESSING";
    private static final Duration RESULT_TTL     = Duration.ofHours(24);
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(2);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final StringRedisTemplate redis;

    public IdempotencyService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String checkOrReverse(UUID tenantId, String idempotencyKey, String requestFingerprint) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return null;

        String redisKey = redisKey(tenantId, idempotencyKey);
        String existing = redis.opsForValue().get(redisKey);

        if (existing != null) {
            if (PROCESSING.equals(existing)) {
                log.warn("Chave de idempotência em processamento: {}", idempotencyKey);
                throw new IdempotencyConflictException(idempotencyKey);
            }
            StoredResult result = deserialize(existing);
            if (result == null) {
                log.warn("Valor idempotente legado sem fingerprint para key={}; validação não disponível",
                        idempotencyKey);
                return existing;
            }
            if (requestFingerprint != null && !requestFingerprint.equals(result.fp())) {
                throw new IdempotencyConflictException(
                        idempotencyKey,
                        "Idempotency key '%s' already used for a different invoice".formatted(idempotencyKey)
                );
            }
            log.debug("Hit idempotente: {} → analysisId={}", idempotencyKey, result.id());
            return result.id();
        }

        Boolean reserved = redis.opsForValue().setIfAbsent(redisKey, PROCESSING, PROCESSING_TTL);
        if (!Boolean.TRUE.equals(reserved)) {
            throw new IdempotencyConflictException(idempotencyKey);
        }
        releaseReservationOnRollback(tenantId, idempotencyKey);
        log.debug("Chave de idempotência reservada: {}", idempotencyKey);
        return null;
    }


    public String checkOrReverse(UUID tenantId, String idempotencyKey) {
        return checkOrReverse(tenantId, idempotencyKey, null);
    }


    public static String fingerprintOf(UUID tenantId, String nfeAccessKey) {
        if (tenantId == null || nfeAccessKey == null || nfeAccessKey.isBlank()) {
            throw new IllegalArgumentException("tenantId e nfeAccessKey são obrigatórios para o fingerprint");
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((tenantId + ":" + nfeAccessKey).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    public void confirmAfterCommit(UUID tenantId, String idempotencyKey, String analysisId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        confirm(tenantId, idempotencyKey, analysisId);
                    } catch (RuntimeException ex) {
                        log.error("Falha ao publicar resultado idempotente após commit key={}: {}",
                                idempotencyKey, ex.getMessage());
                        try {
                            release(tenantId, idempotencyKey);
                        } catch (RuntimeException releaseException) {
                            log.error("Falha ao liberar reserva idempotente key={}: {}",
                                    idempotencyKey, releaseException.getMessage());
                        }
                    }
                }
            });
            return;
        }
        confirm(tenantId, idempotencyKey, analysisId);
    }

    public void confirmAfterCommit(
            UUID tenantId,
            String idempotencyKey,
            String analysisId,
            String requestFingerprint
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    confirm(tenantId, idempotencyKey, analysisId, requestFingerprint);
                }
            });
            return;
        }
        confirm(tenantId, idempotencyKey, analysisId, requestFingerprint);
    }

    private void confirm(UUID tenantId, String idempotencyKey, String analysisId) {
        redis.opsForValue().set(redisKey(tenantId, idempotencyKey), analysisId, RESULT_TTL);
        log.debug("Chave de idempotência confirmada: {} → analysisId={}", idempotencyKey, analysisId);
    }


    public void confirm(UUID tenantId, String idempotencyKey, String analysisId, String requestFingerprint) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        try {
            String value = JSON.writeValueAsString(new StoredResult(analysisId, requestFingerprint));
            redis.opsForValue().set(redisKey(tenantId, idempotencyKey), value, RESULT_TTL);
            log.debug("Chave de idempotência confirmada: {} → analysisId={}", idempotencyKey, analysisId);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar resultado idempotente", e);
        }
    }

    private StoredResult deserialize(String value) {
        if (!value.startsWith("{")) return null;
        try {
            StoredResult result = JSON.readValue(value, StoredResult.class);
            return result.id() == null || result.id().isBlank() || result.fp() == null || result.fp().isBlank()
                    ? null
                    : result;
        } catch (JsonProcessingException e) {
            log.warn("Valor idempotente inválido; tratando como legado: {}", e.getOriginalMessage());
            return null;
        }
    }

    private void releaseReservationOnRollback(UUID tenantId, String idempotencyKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    release(tenantId, idempotencyKey);
                }
            }
        });
    }

    public void release(UUID tenantId, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;

        redis.delete(redisKey(tenantId, idempotencyKey));
        log.debug("Chave de idempotência liberada após erro: {}", idempotencyKey);
    }

    private String redisKey(UUID tenantId, String idempotencyKey) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId é obrigatório para idempotência");
        }
        return PREFIX + tenantId + ":" + idempotencyKey;
    }

    private record StoredResult(String id, String fp) {}
}
