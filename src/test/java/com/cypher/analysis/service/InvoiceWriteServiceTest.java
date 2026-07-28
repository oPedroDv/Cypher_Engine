package com.cypher.analysis.service;

import com.cypher.analysis.domain.FinancialMetrics;
import com.cypher.analysis.domain.Invoice;
import com.cypher.analysis.repository.InvoiceRepository;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.shared.exception.DuplicateInvoiceException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceWriteServiceTest {

    private static final String CHAVE_NFE = "12345678901234567890123456789012345678901234";
    private static final UUID TENANT_ID = UUID.randomUUID();

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private RiskAnalysisRepository riskAnalysisRepository;

    @InjectMocks
    private InvoiceWriteService service;

    @Test
    void translatesNfeKeyUniqueViolationToDuplicateInvoice() {
        when(invoiceRepository.saveAndFlush(any(Invoice.class)))
                .thenThrow(integrityViolation("uq_invoice_tenant_chave_nfe"));

        assertThatThrownBy(this::persist).isInstanceOf(DuplicateInvoiceException.class);
    }

    @Test
    void propagatesUnrelatedIntegrityViolations() {
        when(invoiceRepository.saveAndFlush(any(Invoice.class)))
                .thenThrow(integrityViolation("invoice_issuer_cnpj_not_null"));

        assertThatThrownBy(this::persist)
                .isInstanceOf(DataIntegrityViolationException.class)
                .isNotInstanceOf(DuplicateInvoiceException.class);
    }

    private void persist() {
        service.persist(
                Invoice.of("<nfe/>", TENANT_ID, CHAVE_NFE),
                TENANT_ID,
                0.25,
                "model-v1",
                List.of(),
                metrics(),
                false);
    }

    private DataIntegrityViolationException integrityViolation(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
                "violação de integridade", new SQLException("erro"), constraintName);
        return new DataIntegrityViolationException("could not execute statement", cause);
    }

    private FinancialMetrics metrics() {
        return FinancialMetrics.calculate(
                new BigDecimal("10000.00"), new BigDecimal("8500.00"), 3.2, 0.25,
                new BigDecimal("0.18"), new BigDecimal("0.15"),
                new BigDecimal("0.95"), new BigDecimal("2.50"));
    }
}
