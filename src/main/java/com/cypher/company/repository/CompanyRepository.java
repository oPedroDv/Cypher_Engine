package com.cypher.company.repository;

import com.cypher.company.domain.Company;
import com.cypher.company.domain.CnpjStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByCnpjAndTenantId(String cnpj, UUID tenantId);

    boolean existsByCnpjAndTenantId(String cnpj, UUID tenantId);

    @Query("SELECT c.cnpjStatus FROM Company c WHERE c.cnpj = :cnpj AND c.tenantId = :tenantId")
    Optional<CnpjStatus> findCnpjStatusByCnpjAndTenantId(
            @Param("cnpj") String cnpj,
            @Param("tenantId") UUID tenantId
    );

    @Query("""
            SELECT c FROM Company c
            WHERE c.statusCheckedAt IS NULL OR c.statusCheckedAt < :threshold
            ORDER BY c.statusCheckedAt ASC NULLS FIRST
            """)
    List<Company> findStale(@Param("threshold") Instant threshold);

    List<Company> findByTenantId(UUID tenantId);

    List<Company> findByCnpjStatusAndTenantId(CnpjStatus status, UUID tenantId);

    @Query("SELECT COUNT(c) FROM Company c WHERE c.tenantId = :tenantId AND c.cnpjStatus = 'ACTIVE'")
    long countActiveByTenantId(@Param("tenantId") UUID tenantId);

}