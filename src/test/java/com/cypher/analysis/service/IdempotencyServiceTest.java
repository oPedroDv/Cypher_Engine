package com.cypher.analysis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final String REDIS_KEY = "cypher:idempotency:request-1";

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(redis);
    }

    @Test
    void checkOrReverseReturnsNullAndReservesNewKey() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn(null);
        when(valueOperations.setIfAbsent(REDIS_KEY, "PROCESSING", Duration.ofMinutes(2))).thenReturn(true);

        String result = service.checkOrReverse("request-1");

        assertThat(result).isNull();
        verify(valueOperations).setIfAbsent(REDIS_KEY, "PROCESSING", Duration.ofMinutes(2));
    }

    @Test
    void checkOrReverseReturnsExistingAnalysisId() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn("analysis-123");

        String result = service.checkOrReverse("request-1");

        assertThat(result).isEqualTo("analysis-123");
        verify(valueOperations, never()).setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), eq(Duration.ofMinutes(2)));
    }

    @Test
    void checkOrReverseThrowsConflictWhenKeyIsProcessing() {
        when(redis.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(REDIS_KEY)).thenReturn("PROCESSING");

        assertThatThrownBy(() -> service.checkOrReverse("request-1"))
                .isInstanceOf(IdempotencyService.IdempotencyConflictException.class)
                .hasMessageContaining("Análise em andamento");
    }

    @Test
    void confirmStoresAnalysisIdWithResultTtl() {
        when(redis.opsForValue()).thenReturn(valueOperations);

        service.confirm("request-1", "analysis-123");

        verify(valueOperations).set(REDIS_KEY, "analysis-123", Duration.ofHours(24));
    }

    @Test
    void releaseDeletesReservedKey() {
        service.release("request-1");

        verify(redis).delete(REDIS_KEY);
    }

    @Test
    void blankKeysDoNotTouchRedis() {
        assertThat(service.checkOrReverse(" ")).isNull();

        service.confirm(null, "analysis-123");
        service.release("");

        verify(redis, never()).opsForValue();
        verify(redis, never()).delete("");
    }
}
