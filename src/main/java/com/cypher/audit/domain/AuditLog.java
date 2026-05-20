package com.cypher.audit.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_occurred_at",  columnList = "occurred_at DESC"),
                @Index(name = "idx_audit_action",       columnList = "action"),
                @Index(name = "idx_audit_entity",       columnList = "entity_type, entity_id"),
                @Index(name = "idx_audit_actor",        columnList = "actor_id"),
                @Index(name = "idx_audit_correlation",  columnList = "correlation_id"),
                @Index(name = "idx_audit_success",      columnList = "success"),
                @Index(name = "idx_audit_tenant",       columnList = "tenant_id")
        }
)
public class AuditLog {

        @Id
        @Column(updatable = false, nullable = false)
        private UUID id;

        @Enumerated(EnumType.STRING)
        @Column(name = "action", nullable = false, length = 64)
        private AuditAction action;

        @Column(name = "description", length = 1024)
        private String description;

        @Column(name = "success", nullable = false)
        private boolean success;

        @Column(name = "error_message", length = 2048)
        private String errorMessage;

        @Column(name = "entity_type", length = 64)
        private String entityType;

        @Column(name = "entity_id", length = 256)
        private String entityId;

        @Column(name = "actor_id", length = 256)
        private String actorId;

        @Column(name = "actor_type", length = 32)
        private String actorType;

        @Column(name = "correlation_id", length = 128)
        private String correlationId;

        @Column(name = "tenant_id", length = 128)
        private String tenantId;

        @Column(name = "source_ip", length = 45)
        private String sourceIp;

        @Column(name = "http_endpoint", length = 256)
        private String httpEndpoint;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(name = "metadata", columnDefinition = "jsonb")
        private Map<String, String> metadata;

        @Column(name = "occurred_at", nullable = false, updatable = false)
        private Instant occurredAt;

        protected AuditLog() {}

        private AuditLog(Builder builder) {
                this.id            = Objects.requireNonNull(builder.id, "id não pode ser nulo");
                this.action        = Objects.requireNonNull(builder.action, "action não pode ser nulo");
                this.description   = builder.description;
                this.success       = builder.success;
                this.errorMessage  = builder.errorMessage;
                this.entityType    = builder.entityType;
                this.entityId      = builder.entityId;
                this.actorId       = builder.actorId;
                this.actorType     = builder.actorType;
                this.correlationId = builder.correlationId;
                this.tenantId      = builder.tenantId;
                this.sourceIp      = builder.sourceIp;
                this.httpEndpoint  = builder.httpEndpoint;
                this.metadata      = builder.metadata;
                this.occurredAt    = builder.occurredAt != null ? builder.occurredAt : Instant.now();
        }

        public static Builder builder(AuditAction action) {
                return new Builder(action);
        }

        public UUID getId()              { return id; }
        public AuditAction getAction()   { return action; }
        public String getDescription()   { return description; }
        public boolean isSuccess()       { return success; }
        public String getErrorMessage()  { return errorMessage; }
        public String getEntityType()    { return entityType; }
        public String getEntityId()      { return entityId; }
        public String getActorId()       { return actorId; }
        public String getActorType()     { return actorType; }
        public String getCorrelationId() { return correlationId; }
        public String getTenantId()      { return tenantId; }
        public String getSourceIp()      { return sourceIp; }
        public String getHttpEndpoint()  { return httpEndpoint; }
        public Map<String, String> getMetadata() { return metadata; }
        public Instant getOccurredAt()   { return occurredAt; }

        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof AuditLog that)) return false;
                return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
                return Objects.hash(id);
        }

        @Override
        public String toString() {
                return "AuditLog{id=%s, action=%s, entity=%s/%s, actor=%s, success=%s, at=%s}"
                        .formatted(id, action, entityType, entityId, actorId, success, occurredAt);
        }

        public static final class Builder {

                private UUID id = UUID.randomUUID();
                private final AuditAction action;
                private String description;
                private boolean success = true;
                private String errorMessage;
                private String entityType;
                private String entityId;
                private String actorId = "SYSTEM";
                private String actorType = "SYSTEM";
                private String correlationId;
                private String tenantId;
                private String sourceIp;
                private String httpEndpoint;
                private Map<String, String> metadata;
                private Instant occurredAt;

                private Builder(AuditAction action) {
                        this.action = Objects.requireNonNull(action);
                }

                public Builder id(UUID id)                       { this.id = id; return this; }
                public Builder description(String d)             { this.description = d; return this; }
                public Builder success(boolean s)                { this.success = s; return this; }
                public Builder failure(String errorMessage)      { this.success = false; this.errorMessage = errorMessage; return this; }
                public Builder entity(String type, String id)    { this.entityType = type; this.entityId = id; return this; }
                public Builder actor(String id, String type)     { this.actorId = id; this.actorType = type; return this; }
                public Builder correlationId(String cid)         { this.correlationId = cid; return this; }
                public Builder tenantId(String tid)              { this.tenantId = tid; return this; }
                public Builder sourceIp(String ip)               { this.sourceIp = ip; return this; }
                public Builder httpEndpoint(String endpoint)     { this.httpEndpoint = endpoint; return this; }
                public Builder metadata(Map<String, String> m)   { this.metadata = m; return this; }
                public Builder occurredAt(Instant t)             { this.occurredAt = t; return this; }

                public AuditLog build() {
                        return new AuditLog(this);
                }
        }
}