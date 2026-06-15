package com.cypher.infrastructure.security;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "api_keys",
        indexes = {
                @Index(name = "idx_api_keys_tenant", columnList = "tenant_id"),
                @Index(name = "idx_api_keys_hash", columnList = "key_hash")
        }
)
@Getter
@NoArgsConstructor
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public ApiKey(UUID tenantId, String keyHash, String name) {
        this.tenantId = tenantId;
        this.keyHash  = keyHash;
        this.name     = name;
    }

    public void revoke() {
        this.active    = false;
        this.revokedAt = Instant.now();
    }
}
