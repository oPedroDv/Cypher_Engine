
package com.cypher.infrastructure.persistence;

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
import java.util.UUID;

@Slf4j
@Configuration
public class TenantFilterConfig {

    @Bean
    public TenantFilter tenantFilter() {
        return new TenantFilter();
    }

    private static final class TenantFilter extends OncePerRequestFilter {

        private static final String TENANT_CLAIM = "tenant_id";


        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {
            try {
                extractTenantId().ifPresentOrElse(
                        tenantId -> {
                            TenantContext.set(tenantId);
                            log.debug("TenantContext populado: tenantId={}", tenantId);
                        },
                        () -> log.debug("Request sem tenant_id (Endpoint publico ou não autenticado")
                );

                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        }

        private java.util.Optional<UUID> extractTenantId() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()) {
                return java.util.Optional.empty();
            }

            if (!(auth.getPrincipal() instanceof Jwt jwt)) {
                return java.util.Optional.empty();
            }

            String tenantClaim = jwt.getClaimAsString(TENANT_CLAIM);
            if(tenantClaim == null || tenantClaim.isBlank()) {
                log.warn("JWT autenticado sem claim '{}' - possivelmente token mal formado", TENANT_CLAIM);
                return java.util.Optional.empty();
            }

            try {
                return java.util.Optional.of(UUID.fromString(tenantClaim));
            } catch (IllegalArgumentException e) {
                log.error("tenant_id no JWT não é UUID válido '{}'", tenantClaim);
                return java.util.Optional.empty();
            }
        }
        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();
            return path.startsWith("/actuator");
        }
    }
}
