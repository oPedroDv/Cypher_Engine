package com.cypher.analysis.repository;

import com.cypher.analysis.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByNfeKey(String nfeKey);
}
