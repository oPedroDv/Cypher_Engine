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
            WHERE c.tenantId = :tenantId
              AND (c.statusCheckedAt IS NULL OR c.statusCheckedAt < :threshold)
            ORDER BY c.statusCheckedAt ASC NULLS FIRST
            """)
    List<Company> findDesatualizadas(
            @Param("threshold") Instant threshold,
            @Param("tenantId") UUID tenantId
    );
    List<Company> findByTenantId(UUID tenantId);
    List<Company> findByCnpjStatusAndTenantId(CnpjStatus status, UUID tenantId);

    @Query("SELECT COUNT(c) FROM Company c WHERE c.tenantId = :tenantId AND c.cnpjStatus = 'ATIVA'")
    long countAtivasByTenantId(@Param("tenantId") UUID tenantId);
    List<Company> findByCnpj(String cnpj);
}