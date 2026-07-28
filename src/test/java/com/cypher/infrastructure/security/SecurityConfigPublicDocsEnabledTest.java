package com.cypher.infrastructure.security;

import com.cypher.infrastructure.config.SecurityConfig;
import com.cypher.infrastructure.persistence.TenantFilterConfig;
import com.cypher.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cobre o caminho oposto de {@link SecurityConfigTest#apiDocsEndpointRequiresAuthenticationByDefault()}:
 * com cypher.security.public-docs.enabled=true (uso pretendido apenas em
 * dev/local), os endpoints de Swagger/OpenAPI voltam a ficar públicos.
 * Achado 4.3 da auditoria.
 */
@WebMvcTest(controllers = SecurityProbeController.class)
@Import({SecurityConfig.class, TenantFilterConfig.class, ApiKeyAuthProvider.class, GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "cypher.security.jwt.secret=test-secret-with-at-least-32-bytes",
        "cypher.security.jwt.issuer=https://issuer.test",
        "cypher.security.jwt.audience=cypher-api",
        "cypher.security.public-docs.enabled=true"
})
class SecurityConfigPublicDocsEnabledTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApiKeyRepository apiKeyRepository;

    @Test
    void apiDocsEndpointIsPublicWhenExplicitlyEnabled() throws Exception {
        mockMvc.perform(get("/v3/api-docs/probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));
    }
}
