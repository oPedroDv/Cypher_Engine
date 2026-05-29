package com.cypher.outcome.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "outcomes")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Outcome {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "analysis_id", nullable = false)
    private UUID analysisId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome_type", nullable = false)
    private OutcomeType outcomeType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "amount_received", precision = 19, scale = 2)
    private BigDecimal amountReceived;

    @Column(name = "days_late")
    private Integer daysLate;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Outcome of(UUID analysisId, UUID tenantId, OutcomeType outcomeType,
                             LocalDate eventDate, BigDecimal amountReceived,
                             Integer daysLate, String notes) {
        return Outcome.builder()
                .analysisId(analysisId)
                .tenantId(tenantId)
                .outcomeType(outcomeType)
                .eventDate(eventDate)
                .amountReceived(amountReceived)
                .daysLate(daysLate)
                .notes(notes)
                .build();
    }
}
