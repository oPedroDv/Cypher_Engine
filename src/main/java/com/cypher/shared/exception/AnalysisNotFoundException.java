package com.cypher.shared.exception;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class AnalysisNotFoundException extends CypherException {

    public AnalysisNotFoundException(UUID analysisId) {
        super(
                "Análise não encontrada: " + analysisId,
                HttpStatus.NOT_FOUND,
                "ANALYSIS_NOT_FOUND"
        );
    }
}