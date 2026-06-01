package com.cypher.testutil;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

public class TestEntityHelper {

    public static <T> T withId(T entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    public static <T> T withId(T entity) {
        return withId(entity, UUID.randomUUID());
    }

    public static <T> T withCreatedAt(T entity, Instant createdAt) {
        ReflectionTestUtils.setField(entity, "createdAt", createdAt);
        return entity;
    }

    public static <T> T withCreatedAt(T entity) {
        return withCreatedAt(entity, Instant.now());
    }
}
