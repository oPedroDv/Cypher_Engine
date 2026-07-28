package com.cypher.analysis.service;

import com.cypher.analysis.domain.FinancialMetrics;
import com.cypher.analysis.domain.Invoice;
import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.domain.RiskFactor;
import com.cypher.analysis.repository.InvoiceRepository;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.shared.exception.DuplicateInvoiceException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceWriteService {
    private final InvoiceRepository invoiceRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;

    private static final String NFE_KEY_CONSTRAINT = "uq_invoice_tenant_chave_nfe";

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            timeoutString = "${cypher.persistence.write-timeout-seconds:15}")
    public PersistedAnalysis persist(
            Invoice invoice,
            UUID tenantId,
            double score,
            String modelVersion,
            List<RiskFactor> factors,
            FinancialMetrics metrics,
            boolean dataPartial
    ) {
        try {
            Invoice savedInvoice = invoiceRepository.saveAndFlush(invoice);
            RiskAnalysis analysis = RiskAnalysis.of(savedInvoice, tenantId, score, modelVersion,
                    factors, metrics, dataPartial);
            RiskAnalysis savedAnalysis = riskAnalysisRepository.saveAndFlush(analysis);
            return new PersistedAnalysis(savedInvoice, savedAnalysis);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateNfeKey(ex)) {
                throw new DuplicateInvoiceException(invoice.getNfeKey(), null);
            }
            throw ex;
        }
    }

    /**
     * Somente a violação do índice único {@code (tenant_id, chave_nfe)} representa NF-e duplicada;
     * qualquer outra violação de integridade indica defeito de schema ou de dados e deve propagar.
     */
    private boolean isDuplicateNfeKey(DataIntegrityViolationException ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation
                    && violation.getConstraintName() != null) {
                return violation.getConstraintName().toLowerCase().contains(NFE_KEY_CONSTRAINT);
            }
        }
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.toLowerCase().contains(NFE_KEY_CONSTRAINT);
    }

    public record PersistedAnalysis(Invoice invoice, RiskAnalysis analysis) {}
}
