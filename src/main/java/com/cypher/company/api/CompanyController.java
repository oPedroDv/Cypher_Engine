package com.cypher.company.api;

import com.cypher.company.api.dto.CompanyProfileResponse;
import com.cypher.company.domain.Company;
import com.cypher.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/{cnpj}")
    public ResponseEntity<CompanyProfileResponse> getProfile(
            @PathVariable String cnpj,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID tenantId = extractTenantId(jwt);
        log.debug("Buscando perfil cnpj={} tenant={}", cnpj, tenantId);

        Company company = companyService.resolveCompany(cnpj, tenantId);
        return ResponseEntity.ok(CompanyProfileResponse.from(company));
    }

    @GetMapping("/{cnpj}/status")
    public ResponseEntity<StatusResponse> getStatus(
            @PathVariable String cnpj,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID tenantId = extractTenantId(jwt);
        var status = companyService.checkStatus(cnpj, tenantId);
        return ResponseEntity.ok(new StatusResponse(cnpj, status.name(), status.isFit(), status.isCritical()));
    }

    @PostMapping("/{cnpj}/refresh")
    public ResponseEntity<CompanyProfileResponse> refreshStatus(
            @PathVariable String cnpj,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID tenantId = extractTenantId(jwt);
        log.info("Refresh forçado cnpj={} tenant={}", cnpj, tenantId);

        Company company = companyService.forceStatusRefresh(cnpj, tenantId);
        return ResponseEntity.ok(CompanyProfileResponse.from(company));
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null || tenantClaim.isBlank()) {
            throw new IllegalArgumentException("JWT não contém claim 'tenant_id'");
        }
        return UUID.fromString(tenantClaim);
    }

    public record StatusResponse(
            String cnpj,
            String status,
            boolean fit,
            boolean critical
    ) {}
}
