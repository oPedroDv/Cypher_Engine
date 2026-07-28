package com.cypher.company.service;

import com.cypher.company.domain.CnpjStatus;
import com.cypher.company.domain.Company;
import com.cypher.company.repository.CompanyRepository;
import com.cypher.shared.exception.CompanyNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    private static final String CNPJ = "12345678000190";

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private FederalRevenueClient federalRevenueClient;

    @InjectMocks
    private CompanyService service;

    private final UUID tenantId = UUID.randomUUID();

    @Test
    void resolveCompanyCreatesCompanyFromFederalRevenueDataWhenUnknown() {
        when(companyRepository.findByCnpjAndTenantId(CNPJ, tenantId)).thenReturn(Optional.empty());
        when(federalRevenueClient.query(CNPJ)).thenReturn(
                new FederalRevenueClient.CnpjData(CNPJ, "CEDENTE LTDA", "CEDENTE", CnpjStatus.ACTIVE));
        when(companyRepository.save(any(Company.class))).thenAnswer(call -> call.getArgument(0));

        Company resolved = service.resolveCompany(CNPJ, tenantId);

        assertThat(resolved.getCnpj()).isEqualTo(CNPJ);
        assertThat(resolved.getLegalName()).isEqualTo("CEDENTE LTDA");
        assertThat(resolved.getTradeName()).isEqualTo("CEDENTE");
        assertThat(resolved.getTenantId()).isEqualTo(tenantId);
        assertThat(resolved.getCnpjStatus()).isEqualTo(CnpjStatus.ACTIVE);
        assertThat(resolved.getStatusCheckedAt()).isNotNull();
    }

    @Test
    void resolveCompanyFallsBackToUnknownWhenFederalRevenueFails() {
        when(companyRepository.findByCnpjAndTenantId(CNPJ, tenantId)).thenReturn(Optional.empty());
        when(federalRevenueClient.query(CNPJ)).thenThrow(new IllegalStateException("timeout"));
        when(companyRepository.save(any(Company.class))).thenAnswer(call -> call.getArgument(0));

        Company resolved = service.resolveCompany(CNPJ, tenantId);

        assertThat(resolved.getLegalName()).isEqualTo("EMPRESA DESCONHECIDA");
        assertThat(resolved.getCnpjStatus()).isEqualTo(CnpjStatus.UNKNOWN);
    }

    @Test
    void resolveCompanyRefreshesStaleStatus() {
        Company stale = companyWithStatusCheckedAt(CnpjStatus.SUSPENDED,
                Instant.now().minus(48, ChronoUnit.HOURS));
        when(companyRepository.findByCnpjAndTenantId(CNPJ, tenantId)).thenReturn(Optional.of(stale));
        when(federalRevenueClient.query(CNPJ)).thenReturn(
                new FederalRevenueClient.CnpjData(CNPJ, "CEDENTE LTDA", null, CnpjStatus.ACTIVE));

        Company resolved = service.resolveCompany(CNPJ, tenantId);

        assertThat(resolved.getCnpjStatus()).isEqualTo(CnpjStatus.ACTIVE);
        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertThat(captor.getValue().getStatusCheckedAt()).isAfter(Instant.now().minusSeconds(60));
    }

    @Test
    void resolveCompanyKeepsFreshStatusWithoutExternalQuery() {
        Company fresh = companyWithStatusCheckedAt(CnpjStatus.ACTIVE, Instant.now());
        when(companyRepository.findByCnpjAndTenantId(CNPJ, tenantId)).thenReturn(Optional.of(fresh));

        Company resolved = service.resolveCompany(CNPJ, tenantId);

        assertThat(resolved).isSameAs(fresh);
        verify(federalRevenueClient, never()).query(CNPJ);
        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void findByCnpjThrowsWhenCompanyIsAbsent() {
        when(companyRepository.findByCnpjAndTenantId(CNPJ, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCnpj(CNPJ, tenantId))
                .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    void checkStatusFallsBackToUnknownWhenNotStored() {
        when(companyRepository.findCnpjStatusByCnpjAndTenantId(CNPJ, tenantId))
                .thenReturn(Optional.empty());

        assertThat(service.checkStatus(CNPJ, tenantId)).isEqualTo(CnpjStatus.UNKNOWN);
    }

    @Test
    void checkStatusReturnsStoredStatus() {
        when(companyRepository.findCnpjStatusByCnpjAndTenantId(CNPJ, tenantId))
                .thenReturn(Optional.of(CnpjStatus.UNFIT));

        assertThat(service.checkStatus(CNPJ, tenantId)).isEqualTo(CnpjStatus.UNFIT);
    }

    @Test
    void forceStatusRefreshQueriesFederalRevenueEvenForFreshCompany() {
        Company fresh = companyWithStatusCheckedAt(CnpjStatus.ACTIVE, Instant.now());
        when(companyRepository.findByCnpjAndTenantId(CNPJ, tenantId)).thenReturn(Optional.of(fresh));
        when(federalRevenueClient.query(CNPJ)).thenReturn(
                new FederalRevenueClient.CnpjData(CNPJ, "CEDENTE LTDA", null, CnpjStatus.CLOSED));

        Company refreshed = service.forceStatusRefresh(CNPJ, tenantId);

        assertThat(refreshed.getCnpjStatus()).isEqualTo(CnpjStatus.CLOSED);
        verify(companyRepository).save(fresh);
    }

    @Test
    void listStaleUsesTwentyFourHourThreshold() {
        Company stale = companyWithStatusCheckedAt(CnpjStatus.ACTIVE, null);
        when(companyRepository.findStale(any(Instant.class))).thenReturn(List.of(stale));

        assertThat(service.listStale()).containsExactly(stale);

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(companyRepository).findStale(captor.capture());
        assertThat(captor.getValue()).isBetween(
                Instant.now().minus(25, ChronoUnit.HOURS),
                Instant.now().minus(23, ChronoUnit.HOURS));
    }

    private Company companyWithStatusCheckedAt(CnpjStatus status, Instant statusCheckedAt) {
        return Company.builder()
                .cnpj(CNPJ)
                .legalName("CEDENTE LTDA")
                .tenantId(tenantId)
                .cnpjStatus(status)
                .statusCheckedAt(statusCheckedAt)
                .build();
    }
}
