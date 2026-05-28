package com.cypher.analysis.application.port;

import com.cypher.analysis.domain.Invoice;

import java.util.Optional;

public interface InvoicePersistencePort {

    Optional<Invoice> findByNfeKey(String nfeKey);

    Invoice save(Invoice invoice);
}
