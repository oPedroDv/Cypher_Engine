package com.cypher.shared.exception;

import org.springframework.http.HttpStatus;

public class DuplicateInvoiceException extends CypherException {

    private final String chaveNfe;
    private final String existingAnalysisId;

    public DuplicateInvoiceException(String chaveNfe, String existingAnalysisId) {
        String.format("NF-e com chave '%s' já foi analisada. Use o ID existente: %s", chaveNfe, existingAnalysisId,
                HttpStatus.CONFLICT, "DUPLICATE_INVOICE");
        this.chaveNfe = chaveNfe;
        this.existingAnalysisId = existingAnalysisId;
    }

    public String getChaveNfe() {return chaveNfe;}
    public String getExistingAnalysisId() {return existingAnalysisId;}
}