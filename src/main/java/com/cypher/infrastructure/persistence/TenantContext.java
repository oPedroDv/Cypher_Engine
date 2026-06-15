package com.cypher.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;


public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID get() {
        return CURRENT_TENANT.get();
    }

    public static UUID getRequired() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext não foi populado para a requisição atual");
        }
        return tenantId;
    }

    public static Optional<UUID> getOptional() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
