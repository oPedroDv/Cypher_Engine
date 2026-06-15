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
                extractTenantId(request).ifPresentOrElse(
                        tenantId -> {
                            TenantContext.set(tenantId);
                            log.debug("TenantContext populado: tenantId={}", tenantId);
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
                if (tenantId != null) {
                    return Optional.of(tenantId);
                }
            }

            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Jwt jwt) {
                String claim = jwt.getClaimAsString(TENANT_CLAIM);
                if (claim != null && !claim.isBlank()) {
                    try {
                        return Optional.of(UUID.fromString(claim));
                    } catch (IllegalArgumentException e) {
                        log.error("tenant_id no JWT não é UUID válido: '{}'", claim);
                    }
                } else {
                    log.warn("JWT autenticado sem claim '{}' — token mal formado", TENANT_CLAIM);
                }
                return Optional.empty();
            }

            return Optional.empty();
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return request.getRequestURI().startsWith("/actuator");
        }
    }
}
