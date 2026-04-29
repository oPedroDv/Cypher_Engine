package com.cypher.analysis.service;

import com.cypher.analysis.api.dto.AnalysisRequest;
import com.cypher.analysis.api.dto.AnalysisResponse;
import com.cypher.analysis.domain.Invoice;
import com.cypher.analysis.domain.RiskAnalysis;
import com.cypher.analysis.repository.InvoiceRepository;
import com.cypher.analysis.repository.RiskAnalysisRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class AnalysisService {

    private static final String MODEL_VERSION = "stub_v0.1";

    private final InvoiceRepository invoiceRepository;
    private final RiskAnalysisRepository riskRepository;

    public AnalysisService(InvoiceRepository invoiceRepository,
                           RiskAnalysisRepository riskRepository) {
        this.invoiceRepository = invoiceRepository;
        this.riskRepository = riskRepository;
    }

    @Transactional
    public AnalysisResponse analyze(AnalysisRequest request) {
        Invoice invoice = Invoice.of(request.xmlBase64());
        invoiceRepository.save(invoice);

        double score = computeScore(request.xmlBase64());

        RiskAnalysis analysis = RiskAnalysis.of(invoice, score, MODEL_VERSION);
            riskRepository.save(analysis);

            return AnalysisResponse.from(analysis);
    }

    @Transactional(readOnly = true)
    public Optional<AnalysisResponse> findByID(UUID id) {
        return riskRepository.findById(id)
                .map(AnalysisResponse::from);
    }

    private double computeScore(String xml) {
        return new Random().nextDouble();
    }
}