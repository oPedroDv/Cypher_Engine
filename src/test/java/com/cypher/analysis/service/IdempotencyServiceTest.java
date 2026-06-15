package com.cypher.analysis.service;

import com.cypher.shared.exception.IdempotencyConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    private static final String KEY = "req-123";
    private static final java.util.UUID TENANT_ID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String REDIS_KEY = "cypher:idempotency:" + TENANT_ID + ":" + KEY;
    private static final String ANALYSIS_ID = "analysis-456";

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOperations);
        service = new IdempotencyService(redis);
    }

    @Nested
    @DisplayName("checkOrReverse")
    class CheckOrReverse {

        @Test
        @DisplayName("Should return null and reserve key when it doesn't exist")
        void shouldReserveWhenNew() {
            when(valueOperations.get(REDIS_KEY)).thenReturn(null);
            when(valueOperations.setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), any(Duration.class)))
                    .thenReturn(true);

            String result = service.checkOrReverse(TENANT_ID, KEY);

            assertThat(result).isNull();
            verify(valueOperations).setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), eq(Duration.ofMinutes(2)));
        }

        @Test
        @DisplayName("Should return analysisId when key already exists with a result")
        void shouldReturnExistingId() {
            when(valueOperations.get(REDIS_KEY)).thenReturn(ANALYSIS_ID);

            String result = service.checkOrReverse(TENANT_ID, KEY);

            assertThat(result).isEqualTo(ANALYSIS_ID);
            verify(valueOperations, never()).setIfAbsent(any(), any(), any());
        }

        @Test
        @DisplayName("Should throw conflict when key is currently being processed")
        void shouldThrowConflictWhenProcessing() {
            when(valueOperations.get(REDIS_KEY)).thenReturn("PROCESSING");

            assertThatThrownBy(() -> service.checkOrReverse(TENANT_ID, KEY))
                    .isInstanceOf(IdempotencyConflictException.class);
        }

        @Test
        @DisplayName("Should throw conflict when setIfAbsent returns false (race condition)")
        void shouldThrowConflictOnRaceCondition() {
            when(valueOperations.get(REDIS_KEY)).thenReturn(null);
            when(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(false);

            assertThatThrownBy(() -> service.checkOrReverse(TENANT_ID, KEY))
                    .isInstanceOf(IdempotencyConflictException.class);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        @DisplayName("Should return null immediately for invalid keys")
        void shouldIgnoreInvalidKeys(String invalidKey) {
            assertThat(service.checkOrReverse(TENANT_ID, invalidKey)).isNull();
            verifyNoInteractions(redis);
        }
    }

    @Nested
    @DisplayName("confirm")
    class Confirm {

        @Test
        @DisplayName("Should store analysisId with 24h TTL")
        void shouldStoreResult() {
            service.confirm(TENANT_ID, KEY, ANALYSIS_ID);

            verify(valueOperations).set(eq(REDIS_KEY), eq(ANALYSIS_ID), eq(Duration.ofHours(24)));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should do nothing for invalid keys")
        void shouldIgnoreInvalidKeys(String invalidKey) {
            service.confirm(TENANT_ID, invalidKey, ANALYSIS_ID);
            verifyNoInteractions(valueOperations);
        }
    }

    @Nested
    @DisplayName("release")
    class Release {

        @Test
        @DisplayName("Should delete key from redis")
        void shouldDeleteKey() {
            service.release(TENANT_ID, KEY);

            verify(redis).delete(REDIS_KEY);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should do nothing for invalid keys")
        void shouldIgnoreInvalidKeys(String invalidKey) {
            service.release(TENANT_ID, invalidKey);
            verify(redis, never()).delete(anyString());
        }
    }
}
