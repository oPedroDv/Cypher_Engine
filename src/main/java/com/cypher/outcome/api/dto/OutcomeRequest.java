package com.cypher.outcome.api.dto;

import com.cypher.outcome.domain.OutcomeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OutcomeRequest(
        @NotNull(message = "O resultado da operação é obrigatorio")
        OutcomeType outcome,

        @NotNull(message = "A data do evento é obrigatoria")
        @PastOrPresent(message = "A data do evento não pode ser futura")
        LocalDate eventDate,

        BigDecimal amountReceived,

        Integer daysLate,

        @Size(max = 500, message = "Observação deve ter no maximo 500 caracteres")
        String notes
) {}