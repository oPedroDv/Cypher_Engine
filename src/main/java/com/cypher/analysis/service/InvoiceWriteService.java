package com.cypher.analysis.service;

import com.cypher.analysis.domain.FinancialMetrics;
import com.cypher.analysis.domain.Invoice;
import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.domain.RiskFactor;
import com.cypher.analysis.repository.InvoiceRepository;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import com.cypher.shared.exception.DuplicateInvoiceException;
import lombok.RequiredArgsConstructor;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
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
            throw new DuplicateInvoiceException(invoice.getNfeKey(), null);
        }
    }

    public record PersistedAnalysis(Invoice invoice, RiskAnalysis analysis) {}
}
