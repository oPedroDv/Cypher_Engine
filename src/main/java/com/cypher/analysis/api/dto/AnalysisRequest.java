package com.cypher.analysis.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(
        @NotBlank(message = "xmlBase64 é obrigatório.")
        String xmlBase64,
        String idempotencyKey
) {}