package com.cypher.company.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "companies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_companies_cnpj_tenant",
                        columnNames = {"cnpj", "tenant_id"}
                )
        },
        indexes = {
                @Index(name = "idx_companies_cnpj",        columnList = "cnpj"),
                @Index(name = "idx_companies_tenant_id",   columnList = "tenant_id"),
                @Index(name = "idx_companies_cnpj_status", columnList = "cnpj_status")
        }
)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 14, updatable = false)
    private String cnpj;

    @Column(name = "razao_social", nullable = false, length = 256)
    private String legalName;

    @Column(name = "nome_fantasia", length = 256)
    private String tradeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "cnpj_status", nullable = false, length = 32)
    private CnpjStatus cnpjStatus;

    @Column(name = "status_checked_at")
    private Instant statusCheckedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "receita_federal_data", columnDefinition = "jsonb")
    private Map<String, Object> federalRevenueData;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Company() {}

    private Company(Builder builder) {
        this.id                 = builder.id;
        this.cnpj               = builder.cnpj;
        this.legalName          = builder.legalName;
        this.tradeName          = builder.tradeName;
        this.cnpjStatus         = builder.cnpjStatus;
        this.statusCheckedAt    = builder.statusCheckedAt;
        this.federalRevenueData = builder.federalRevenueData;
        this.tenantId           = builder.tenantId;
        this.createdAt          = builder.createdAt;
        this.updatedAt          = builder.updatedAt;
    }

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.cnpjStatus == null) this.cnpjStatus = CnpjStatus.UNKNOWN;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateStatus(CnpjStatus newStatus, Map<String, Object> federalRevenueData) {
        this.cnpjStatus         = newStatus;
        this.federalRevenueData = federalRevenueData;
        this.statusCheckedAt    = Instant.now();
        this.updatedAt          = Instant.now();
    }

    public boolean isStatusStale(long ttlHours) {
        if (statusCheckedAt == null) return true;
        return statusCheckedAt.isBefore(Instant.now().minusSeconds(ttlHours * 3600L));
    }

    public UUID getId()                                { return id; }
    public String getCnpj()                            { return cnpj; }
    public String getLegalName()                       { return legalName; }
    public String getTradeName()                       { return tradeName; }
    public CnpjStatus getCnpjStatus()                  { return cnpjStatus; }
    public Instant getStatusCheckedAt()                { return statusCheckedAt; }
    public Map<String, Object> getFederalRevenueData() { return federalRevenueData; }
    public UUID getTenantId()                          { return tenantId; }
    public Instant getCreatedAt()                      { return createdAt; }
    public Instant getUpdatedAt()                      { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String cnpj;
        private String legalName;
        private String tradeName;
        private CnpjStatus cnpjStatus = CnpjStatus.UNKNOWN;
        private Instant statusCheckedAt;
        private Map<String, Object> federalRevenueData;
        private UUID tenantId;
        private Instant createdAt;
        private Instant updatedAt;

        private Builder() {}

        public Builder id(UUID id)                              { this.id = id; return this; }
        public Builder cnpj(String cnpj)                        { this.cnpj = cnpj; return this; }
        public Builder legalName(String legalName)              { this.legalName = legalName; return this; }
        public Builder tradeName(String tradeName)              { this.tradeName = tradeName; return this; }
        public Builder cnpjStatus(CnpjStatus cnpjStatus)       { this.cnpjStatus = cnpjStatus; return this; }
        public Builder statusCheckedAt(Instant statusCheckedAt) { this.statusCheckedAt = statusCheckedAt; return this; }
        public Builder federalRevenueData(Map<String, Object> data) { this.federalRevenueData = data; return this; }
        public Builder tenantId(UUID tenantId)                  { this.tenantId = tenantId; return this; }
        public Builder createdAt(Instant createdAt)             { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt)             { this.updatedAt = updatedAt; return this; }

        public Company build() {
            if (cnpj == null || cnpj.isBlank())               throw new IllegalStateException("cnpj é obrigatório");
            if (legalName == null || legalName.isBlank())     throw new IllegalStateException("legalName é obrigatório");
            if (tenantId == null)                             throw new IllegalStateException("tenantId é obrigatório");
            return new Company(this);
        }
    }
}
