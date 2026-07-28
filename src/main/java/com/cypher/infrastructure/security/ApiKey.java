package com.cypher.infrastructure.security;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /**
     * Scopes concedidos a esta key, persistidos como string separada por vírgula
     * (ex.: "analysis:read,analysis:write"). Vazio = nenhum scope concedido, ou
     * seja, a key não passa em nenhum endpoint anotado com @RequiresScope.
     * Ver achado 4.1 da auditoria: antes disso, toda API key recebia
     * ROLE_API_CLIENT, que era aceito como bypass universal de scope.
     */
    @Column(name = "scopes", length = 500, nullable = false)
    private String scopes = "";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public ApiKey(UUID tenantId, String keyHash, String name) {
        this(tenantId, keyHash, name, Set.of());
    }

    public ApiKey(UUID tenantId, String keyHash, String name, Set<String> scopes) {
        this.tenantId = tenantId;
        this.keyHash  = keyHash;
        this.name     = name;
        this.scopes   = normalizeScopes(scopes);
    }

    public void revoke() {
        this.active    = false;
        this.revokedAt = Instant.now();
    }

    /**
     * Retorna os scopes desta key como um conjunto imutável. Nunca inclui
     * nenhum bypass implícito — key sem scope configurado retorna conjunto vazio.
     */
    public Set<String> getScopeSet() {
        if (scopes == null || scopes.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalizeScopes(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return "";
        }
        Set<String> cleaned = new LinkedHashSet<>();
        for (String scope : scopes) {
            if (scope != null && !scope.isBlank()) {
                cleaned.add(scope.trim());
            }
        }
        return String.join(",", cleaned);
    }
}
