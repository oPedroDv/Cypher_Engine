package com.cypher.infrastructure.security;

import com.cypher.infrastructure.config.SecurityConfig;
import com.cypher.infrastructure.persistence.TenantFilterConfig;
import com.cypher.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WebMvcTest(controllers = SecurityProbeController.class)
@Import({SecurityConfig.class, TenantFilterConfig.class, ApiKeyAuthProvider.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "cypher.security.jwt.secret=test-secret-with-at-least-32-bytes"
        , "cypher.security.jwt.issuer=https://issuer.test"
        , "cypher.security.jwt.audience=cypher-api"
})
class SecurityConfigTest {

    private static final String JWT_SECRET = "test-secret-with-at-least-32-bytes";
    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String RAW_API_KEY = "cypher_test_api_key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @MockBean
    private ApiKeyRepository apiKeyRepository;

    @Test
    void publicApiDocsEndpointDoesNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs/probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));

        verify(apiKeyRepository, never()).findByKeyHashAndActiveTrue(anyString());
    }

    @Test
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/security/probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointAcceptsJwtAndPopulatesTenantContext() throws Exception {
        mockMvc.perform(get("/api/security/probe")
                        .with(jwt().jwt(token -> token.claim("tenant_id", TENANT_ID.toString()))))
                .andExpect(status().isOk())
                .andExpect(content().string(TENANT_ID.toString()));
    }

    @Test
    void protectedEndpointAcceptsValidApiKeyAndPopulatesTenantContext() throws Exception {
        ApiKey apiKey = new ApiKey(TENANT_ID, ApiKeyAuthProvider.hashKey(RAW_API_KEY), "test-key");
        when(apiKeyRepository.findByKeyHashAndActiveTrue(ApiKeyAuthProvider.hashKey(RAW_API_KEY)))
                .thenReturn(Optional.of(apiKey));

        mockMvc.perform(get("/api/security/probe")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, RAW_API_KEY))
                .andExpect(status().isOk())
                .andExpect(content().string(TENANT_ID.toString()));
    }

    @Test
    void protectedEndpointRejectsInvalidApiKeyBeforeController() throws Exception {
        when(apiKeyRepository.findByKeyHashAndActiveTrue(ApiKeyAuthProvider.hashKey(RAW_API_KEY)))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/security/probe")
                        .header(ApiKeyAuthFilter.API_KEY_HEADER, RAW_API_KEY)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("INVALID_API_KEY"))
                .andExpect(jsonPath("$.message").value("API key inválida ou revogada"));
    }

    @Test
    void corsPreflightAllowsFrontendAuthHeaders() throws Exception {
        mockMvc.perform(options("/api/security/probe")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,X-API-Key,X-Tenant-Id"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void decoderAcceptsOnlyExpectedIssuerAudienceAndValidTenant() {
        String valid = token("https://issuer.test", "cypher-api", TENANT_ID.toString());
        assertThat(jwtDecoder.decode(valid).getSubject()).isEqualTo("test-user");

        assertThatThrownBy(() -> jwtDecoder.decode(token("https://other.test", "cypher-api", TENANT_ID.toString())))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwtDecoder.decode(token("https://issuer.test", "other-api", TENANT_ID.toString())))
                .isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwtDecoder.decode(token("https://issuer.test", "cypher-api", "not-a-uuid")))
                .isInstanceOf(JwtException.class);
    }

    private String token(String issuer, String audience, String tenantId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("test-user")
                .issuer(issuer)
                .audience().add(audience).and()
                .claim("tenant_id", tenantId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
