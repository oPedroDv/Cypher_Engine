package com.cypher.company.service;

import com.cypher.company.domain.Company;
import com.cypher.company.domain.CnpjStatus;
import com.cypher.company.repository.CompanyRepository;
import com.cypher.shared.exception.CompanyNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyService {

    private static final int STATUS_TTL_HOURS = 24;

    private final CompanyRepository companyRepository;
    private final FederalRevenueClient federalRevenueClient;


    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Company resolveCompany(String cnpj, UUID tenantId) {
        log.debug("Resolvendo empresa cnpj={} tenantId={}", cnpj, tenantId);

        Company company = companyRepository
                .findByCnpjAndTenantId(cnpj, tenantId)
                .orElseGet(() -> createCompany(cnpj, tenantId));

        if (company.isStatusStale(STATUS_TTL_HOURS)) {
            log.info("Status desatualizado para cnpj={}, consultando Receita Federal", cnpj);
            updateExternalStatus(company);
        }
        return company;
    }
    @Transactional(readOnly = true)
    public Company findByCnpj(String cnpj, UUID tenantId) {
        return companyRepository
                .findByCnpjAndTenantId(cnpj, tenantId)
                .orElseThrow(() -> new CompanyNotFoundException(cnpj));
    }

    @Transactional(readOnly = true)
    public CnpjStatus checkStatus(String cnpj, UUID tenantId){
        return companyRepository
                .findCnpjStatusByCnpjAndTenantId(cnpj, tenantId)
                .orElse(CnpjStatus.UNKNOWN);
    }

    @Transactional
    public Company forceStatusRefresh(String cnpj, UUID tenantId) {
        log.info("Forçando atualização de status cnpj={} tenant={}", cnpj, tenantId);
        Company company = findByCnpj(cnpj, tenantId);
        updateExternalStatus(company);
        return company;
    }

    @Transactional(readOnly = true)
    public List<Company> listStale() {
        return companyRepository.findStale(Instant.now().minusSeconds(STATUS_TTL_HOURS * 3600L));
    }

    private Company createCompany(String cnpj, UUID tenantId){
        log.info("Criando um novo registro de empresa cnpj={} tenant={}", cnpj, tenantId);

        FederalRevenueClient.CnpjData data = queryFederalRevenue(cnpj);

        Company company = Company.builder()
                .cnpj(cnpj)
                .tenantId(tenantId)
                .legalName(data.legalName())
                .tradeName(data.tradeName())
                .cnpjStatus(data.status())
                .build();

        return companyRepository.save(company);
    }

    private void updateExternalStatus(Company company) {
        FederalRevenueClient.CnpjData data = queryFederalRevenue(company.getCnpj());
        company.updateStatus(data.status(), null);
        companyRepository.save(company);
        log.info("Status atualizado cnpj={} novo={}", company.getCnpj(), data.status());
    }

    private FederalRevenueClient.CnpjData queryFederalRevenue(String cnpj) {
        try {
            return federalRevenueClient.query(cnpj);
        } catch (Exception ex) {
            log.warn("Falha ao consultar Receita federal para cnpj={}, usando fallback. erro={}", cnpj, ex.getMessage());
            return FederalRevenueClient.CnpjData.unknown(cnpj);
        }
    }
}
