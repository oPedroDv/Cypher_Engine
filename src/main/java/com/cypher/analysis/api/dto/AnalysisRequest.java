package com.cypher.analysis.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AnalysisRequest(
        @NotBlank(message = "xmlBase64 é obrigatório")
        @Size(max = 1_450_000, message = "xmlBase64 excede o tamanho máximo permitido")
        String xmlBase64,
        String idempotencyKey,

        @Positive(message = "requestedAdvanceValue deve ser positivo")
        @Digits(integer = 15, fraction = 2, message = "requestedAdvanceValue deve ter no máximo duas casas decimais")
        BigDecimal requestedAdvanceValue,
        double requestedMonthlyRate
) {}
