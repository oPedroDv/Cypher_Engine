package com.cypher.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RestController
@Profile("dev")
@ConditionalOnProperty(name = "cypher.security.dev-token.enabled", havingValue = "true")
public class DevAuthController {

    @Value("${cypher.security.jwt.secret}")
    private String jwtSecret;

    @Value("${cypher.security.jwt.issuer}")
    private String jwtIssuer;

    @Value("${cypher.security.jwt.audience}")
    private String jwtAudience;

    @Value("${cypher.security.dev-token.tenant-id}")
    private UUID tenantId;

    @GetMapping("/dev/token")
    public Map<String, String> generateToken() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        String token = Jwts.builder()
                .subject("dev-user")
                .issuer(jwtIssuer)
                .audience().add(jwtAudience).and()
                .claim("tenant_id", tenantId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
                
        return Map.of("access_token", token, "token_type", "Bearer");
    }
}
