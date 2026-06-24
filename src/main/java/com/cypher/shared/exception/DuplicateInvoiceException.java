package com.cypher.shared.exception;

import org.springframework.http.HttpStatus;

public class DuplicateInvoiceException extends CypherException {

    private final String nfeKey;
    private final String existingAnalysisId;

    public DuplicateInvoiceException(String nfeKey, String existingAnalysisId) {
        super(
                existingAnalysisId == null
                        ? "NF-e com chave '%s' já está sendo ou foi analisada".formatted(nfeKey)
                        : "NF-e com chave '%s' já foi analisada. Use o ID existente: %s"
                                .formatted(nfeKey, existingAnalysisId),
                HttpStatus.CONFLICT,
                "DUPLICATE_INVOICE"
        );
        this.nfeKey             = nfeKey;
        this.existingAnalysisId = existingAnalysisId;
    }

    public String getNfeKey()             { return nfeKey; }
    public String getExistingAnalysisId() { return existingAnalysisId; }
}
