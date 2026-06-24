package com.cypher.infrastructure.security;

import com.cypher.infrastructure.persistence.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SecurityProbeController {

    @GetMapping("/api/security/probe")
    String protectedProbe() {
        return TenantContext.getRequired().toString();
    }

    @GetMapping("/v3/api-docs/probe")
    String publicProbe() {
        return "public";
    }
}
