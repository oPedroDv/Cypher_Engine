package com.cypher.company.api.dto;

import com.cypher.company.domain.CnpjStatus;
import com.cypher.company.domain.Company;

import java.time.LocalDateTime;
import java.util.UUID;

public record CompanyProfileResponse(
        UUID id,
        String cnpj,
        String legalName,
        String tradeName,
        CnpjStatus status,
        boolean fit,
        boolean criticalStatus,
        LocalDateTime statusUpdatedAt,
        LocalDateTime createdAt
) {
    public static CompanyProfileResponse from(Company company){
        return new CompanyProfileResponse(
                company.getId(),
                company.getCnpj(),
                company.getLegalName(),
                company.getTradeName(),
                company.getCnpjStatus(),
                company.getCnpjStatus().isFit(),
                company.getCnpjStatus().isCritical(),
                LocalDateTime.ofInstant(company.getStatusCheckedAt(), java.time.ZoneOffset.UTC),
                LocalDateTime.ofInstant(company.getCreatedAt(), java.time.ZoneOffset.UTC)
        );
    }
}
