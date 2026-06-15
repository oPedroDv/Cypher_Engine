package com.cypher.analysis.repository;

import com.cypher.analysis.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByTenantIdAndNfeKey(UUID tenantId, String nfeKey);

    int countByTenantIdAndIssuerCnpj(UUID tenantId, String issuerCnpj);
    int countByTenantIdAndRecipientCnpj(UUID tenantId, String recipientCnpj);
    int countByTenantIdAndIssuerCnpjAndRecipientCnpj(UUID tenantId, String issuerCnpj, String recipientCnpj);
}
