package com.cypher.infrastructure.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@RestController
@Profile("dev")
public class DevAuthController {

    @Value("${cypher.security.jwt.secret}")
    private String jwtSecret;

    @GetMapping("/dev/token")
    public Map<String, String> generateToken(@RequestParam(defaultValue = "00000000-0000-0000-0000-000000000001") String tenantId) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        
        String token = Jwts.builder()
                .subject("dev-user")
                .claim("tenant_id", tenantId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(key)
                .compact();
                
        return Map.of("access_token", token, "token_type", "Bearer");
    }
}
