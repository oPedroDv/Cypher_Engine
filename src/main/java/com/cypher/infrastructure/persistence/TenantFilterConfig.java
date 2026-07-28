package com.cypher.infrastructure.persistence;

import com.cypher.infrastructure.security.ApiKeyAuthentication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Configuration
public class TenantFilterConfig {

    @Bean
    public TenantFilter tenantFilter() {
        return new TenantFilter();
    }

    public static final class TenantFilter extends OncePerRequestFilter {

        private static final String TENANT_CLAIM   = "tenant_id";
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            try {
                Optional<UUID> tenantId;
                try {
                    tenantId = extractTenantId(request);
                } catch (InvalidTenantException e) {
                    log.error("Requisição autenticada rejeitada: {}", e.getMessage());
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid_tenant");
                    return;
                }

                tenantId.ifPresentOrElse(
                        id -> {
                            TenantContext.set(id);
                            log.debug("TenantContext populado: tenantId={}", id);
                        },
                        () -> log.debug("Request sem tenant_id (endpoint público ou não autenticado)")
                );
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        }

        private Optional<UUID> extractTenantId(HttpServletRequest request) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth instanceof ApiKeyAuthentication apiKeyAuth && apiKeyAuth.isAuthenticated()) {
                UUID tenantId = apiKeyAuth.getTenantId();
                if (tenantId == null) {
                    throw new InvalidTenantException("API key autenticada sem tenantId associado");
                }
                return Optional.of(tenantId);
            }

            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
                String claim = jwt.getClaimAsString(TENANT_CLAIM);
                if (claim == null || claim.isBlank()) {
                    throw new InvalidTenantException(
                            "JWT autenticado sem claim '" + TENANT_CLAIM + "' — token mal formado");
                }
                try {
                    return Optional.of(UUID.fromString(claim));
                } catch (IllegalArgumentException e) {
                    throw new InvalidTenantException("tenant_id no JWT não é UUID válido: '" + claim + "'");
                }
            }

            return Optional.empty();
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return request.getRequestURI().startsWith("/actuator");
        }

        private static final class InvalidTenantException extends RuntimeException {
            InvalidTenantException(String message) {
                super(message);
            }
        }
    }
}
