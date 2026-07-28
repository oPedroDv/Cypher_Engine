package com.cypher.company.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanyTest {

    private static final String CNPJ = "12345678000190";

    @Test
    void builderDefaultsStatusToUnknown() {
        Company company = builder().build();

        assertThat(company.getCnpjStatus()).isEqualTo(CnpjStatus.UNKNOWN);
        assertThat(company.getStatusCheckedAt()).isNull();
        assertThat(company.getFederalRevenueData()).isNull();
    }

    @Test
    void builderStoresEveryProvidedField() {
        UUID id = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        Instant checkedAt = Instant.parse("2024-05-01T10:15:30Z");

        Company company = Company.builder()
                .id(id)
                .cnpj(CNPJ)
                .legalName("CEDENTE LTDA")
                .tradeName("CEDENTE")
                .cnpjStatus(CnpjStatus.ACTIVE)
                .statusCheckedAt(checkedAt)
                .federalRevenueData(Map.of("porte", "ME"))
                .tenantId(tenantId)
                .createdAt(checkedAt)
                .updatedAt(checkedAt)
                .build();

        assertThat(company.getId()).isEqualTo(id);
        assertThat(company.getTradeName()).isEqualTo("CEDENTE");
        assertThat(company.getStatusCheckedAt()).isEqualTo(checkedAt);
        assertThat(company.getFederalRevenueData()).containsEntry("porte", "ME");
        assertThat(company.getTenantId()).isEqualTo(tenantId);
        assertThat(company.getCreatedAt()).isEqualTo(checkedAt);
        assertThat(company.getUpdatedAt()).isEqualTo(checkedAt);
    }

    @Test
    void builderRejectsMissingMandatoryFields() {
        assertThatThrownBy(() -> Company.builder().legalName("X").tenantId(UUID.randomUUID()).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cnpj");

        assertThatThrownBy(() -> Company.builder().cnpj(CNPJ).legalName("  ").tenantId(UUID.randomUUID()).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("legalName");

        assertThatThrownBy(() -> Company.builder().cnpj(CNPJ).legalName("X").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void updateStatusRefreshesStatusDataAndTimestamps() {
        Company company = builder().cnpjStatus(CnpjStatus.ACTIVE).build();

        company.updateStatus(CnpjStatus.CLOSED, Map.of("motivo", "baixa"));

        assertThat(company.getCnpjStatus()).isEqualTo(CnpjStatus.CLOSED);
        assertThat(company.getFederalRevenueData()).containsEntry("motivo", "baixa");
        assertThat(company.getStatusCheckedAt()).isNotNull();
        assertThat(company.getUpdatedAt()).isNotNull();
    }

    @Test
    void statusIsStaleWhenNeverCheckedOrOlderThanTtl() {
        assertThat(builder().build().isStatusStale(24)).isTrue();
        assertThat(builder().statusCheckedAt(Instant.now().minus(48, ChronoUnit.HOURS)).build()
                .isStatusStale(24)).isTrue();
        assertThat(builder().statusCheckedAt(Instant.now()).build().isStatusStale(24)).isFalse();
    }

    @Test
    void cnpjStatusClassifiesFitnessAndCriticality() {
        assertThat(CnpjStatus.ACTIVE.isFit()).isTrue();
        assertThat(CnpjStatus.SUSPENDED.isFit()).isFalse();
        assertThat(CnpjStatus.CLOSED.isCritical()).isTrue();
        assertThat(CnpjStatus.NULLIFIED.isCritical()).isTrue();
        assertThat(CnpjStatus.UNFIT.isCritical()).isFalse();
        assertThat(CnpjStatus.UNKNOWN.isCritical()).isFalse();
    }

    private Company.Builder builder() {
        return Company.builder()
                .cnpj(CNPJ)
                .legalName("CEDENTE LTDA")
                .tenantId(UUID.randomUUID());
    }
}
