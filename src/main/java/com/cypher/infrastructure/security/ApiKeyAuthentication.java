package com.cypher.infrastructure.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ApiKeyAuthentication extends AbstractAuthenticationToken {

    private final String apiKey;
    private final UUID tenantId;

    public ApiKeyAuthentication(String apiKey) {
        super(List.of());
        this.apiKey   = apiKey;
        this.tenantId = null;
        setAuthenticated(false);
    }

    /**
     * Autoridades concedidas correspondem exatamente aos scopes persistidos na
     * API key (prefixo SCOPE_, mesma convenção usada pelo JWT). Não existe mais
     * nenhuma role universal de bypass: uma key sem scopes não é autorizada em
     * nenhum endpoint anotado com @RequiresScope — ver achado 4.1 da auditoria.
     */
    public ApiKeyAuthentication(String apiKey, UUID tenantId, Set<String> scopes) {
        super(toAuthorities(scopes));
        this.apiKey   = apiKey;
        this.tenantId = tenantId;
        setAuthenticated(true);
    }

    private static List<GrantedAuthority> toAuthorities(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return List.of();
        }
        return scopes.stream()
                .map(scope -> (GrantedAuthority) new SimpleGrantedAuthority("SCOPE_" + scope))
                .collect(Collectors.toUnmodifiableList());
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
