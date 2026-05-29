package com.cypher.infrastructure.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final String apiKey;
    private final UUID tenantId;

    public ApiKeyAuthentication(String apiKey) {
        super(List.of());
        this.apiKey   = apiKey;
        this.tenantId = null;
        setAuthenticated(false);
    }

    public ApiKeyAuthentication(String apiKey, UUID tenantId) {
        super(List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT")));
        this.apiKey   = apiKey;
        this.tenantId = tenantId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return tenantId;
    }

    public UUID getTenantId() {
        return tenantId;
    }
}