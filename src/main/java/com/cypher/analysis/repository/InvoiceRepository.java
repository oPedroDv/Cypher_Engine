package com.cypher.analysis.repository;

import com.cypher.analysis.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByTenantIdAndNfeKey(UUID tenantId, String nfeKey);

    int countByTenantIdAndIssuerCnpj(UUID tenantId, String issuerCnpj);
    int countByTenantIdAndRecipientCnpj(UUID tenantId, String recipientCnpj);
    int countByTenantIdAndIssuerCnpjAndRecipientCnpj(UUID tenantId, String issuerCnpj, String recipientCnpj);

    @Query(value = """
            SELECT
                COUNT(*) FILTER (WHERE i.issuer_cnpj = :issuerCnpj) AS issuerTotal,
                COUNT(*) FILTER (WHERE i.recipient_cnpj = :payerCnpj) AS payerTotal,
                COUNT(*) FILTER (WHERE i.issuer_cnpj = :issuerCnpj AND i.recipient_cnpj = :payerCnpj) AS pairTotal,
                (
                    SELECT COALESCE(AVG((ra.financial_metrics ->> 'faceValue')::numeric), 0)
                    FROM risk_analysis ra
                    INNER JOIN invoice ri ON ri.id = ra.invoice_id
                    WHERE ri.issuer_cnpj = :issuerCnpj
                      AND ra.tenant_id = :tenantId
                      AND ra.financial_metrics IS NOT NULL
                ) AS issuerAvgValue
            FROM invoice i
            WHERE i.tenant_id = :tenantId
            """, nativeQuery = true)
    InvoiceHistoryStats summarizeHistory(
            @Param("tenantId") UUID tenantId,
            @Param("issuerCnpj") String issuerCnpj,
            @Param("payerCnpj") String payerCnpj
    );
}
