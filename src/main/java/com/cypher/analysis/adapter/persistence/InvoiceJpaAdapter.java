package com.cypher.analysis.adapter.persistence;

import com.cypher.analysis.application.port.InvoicePersistencePort;
import com.cypher.analysis.domain.Invoice;
import com.cypher.analysis.repository.InvoiceRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class InvoiceJpaAdapter implements InvoicePersistencePort {

    private final InvoiceRepository repository;

    public InvoiceJpaAdapter(InvoiceRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Invoice> findByNfeKey(String nfeKey) {
        return repository.findByNfeKey(nfeKey);
    }

    @Override
    public Invoice save(Invoice invoice) {
        return repository.save(invoice);
    }
}
