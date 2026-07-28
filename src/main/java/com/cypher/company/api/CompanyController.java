package com.cypher.company.api;

import com.cypher.company.api.dto.CompanyProfileResponse;
import com.cypher.company.domain.Company;
import com.cypher.company.service.CompanyService;
import com.cypher.infrastructure.security.RequiresScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import com.cypher.infrastructure.persistence.TenantContext;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @RequiresScope("company:read")
    @GetMapping("/{cnpj}")
    public ResponseEntity<CompanyProfileResponse> getProfile(
            @PathVariable String cnpj
    ) {
        UUID tenantId = TenantContext.getRequired();
        log.debug("Buscando perfil cnpj={} tenant={}", cnpj, tenantId);

        Company company = companyService.resolveCompany(cnpj, tenantId);
        return ResponseEntity.ok(CompanyProfileResponse.from(company));
    }

    @RequiresScope("company:read")
    @GetMapping("/{cnpj}/status")
    public ResponseEntity<StatusResponse> getStatus(
            @PathVariable String cnpj
    ) {
        UUID tenantId = TenantContext.getRequired();
        var status = companyService.checkStatus(cnpj, tenantId);
        return ResponseEntity.ok(new StatusResponse(cnpj, status.name(), status.isFit(), status.isCritical()));
    }

    @RequiresScope("company:write")
    @PostMapping("/{cnpj}/refresh")
    public ResponseEntity<CompanyProfileResponse> refreshStatus(
            @PathVariable String cnpj
    ) {
        UUID tenantId = TenantContext.getRequired();
        log.info("Refresh forçado cnpj={} tenant={}", cnpj, tenantId);

        Company company = companyService.forceStatusRefresh(cnpj, tenantId);
        return ResponseEntity.ok(CompanyProfileResponse.from(company));
    }

    public record StatusResponse(
            String cnpj,
            String status,
            boolean fit,
            boolean critical
    ) {}
}
