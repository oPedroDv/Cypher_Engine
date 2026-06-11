package com.cypher.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthProvider implements AuthenticationProvider {

    private final ApiKeyRepository apiKeyRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        ApiKeyAuthentication apiKeyAuth = (ApiKeyAuthentication) authentication;
        String rawKey = (String) apiKeyAuth.getCredentials();

        if (rawKey == null || rawKey.isBlank()) {
            throw new BadCredentialsException("API key não pode ser vazia");
        }

        String hash = hashKey(rawKey);

        ApiKey apiKey = apiKeyRepository.findByKeyHashAndActiveTrue(hash)
                .orElseThrow(() -> {
                    log.warn("Tentativa de autenticação com API key inválida ou revogada");
                    return new BadCredentialsException("API key inválida ou revogada");
                });

        log.debug("API key autenticada. tenantId={} name={}", apiKey.getTenantId(), apiKey.getName());
        return new ApiKeyAuthentication(rawKey, apiKey.getTenantId());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthentication.class.isAssignableFrom(authentication);
    }

    static String hashKey(String rawKey) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }
}