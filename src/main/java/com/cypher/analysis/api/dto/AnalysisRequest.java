package com.cypher.analysis.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AnalysisRequest(
        @NotBlank(message = "xmlBase64 é obrigatório")
        String xmlBase64,
        String idempotencyKey,

        @Positive(message = "requestedAdvanceValue deve ser positivo")
        BigDecimal requestedAdvanceValue,
        double requestedMonthlyRate
) {}