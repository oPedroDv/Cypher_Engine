CREATE TABLE audit_logs
(
    id             UUID         NOT NULL,
    action         VARCHAR(64)  NOT NULL,
    description    VARCHAR(1024),
    success        BOOLEAN      NOT NULL DEFAULT TRUE,
    error_message  VARCHAR(2048),

    entity_type    VARCHAR(64),
    entity_id      VARCHAR(256),

    actor_id       VARCHAR(256) NOT NULL DEFAULT 'SYSTEM',
    actor_type     VARCHAR(32)  NOT NULL DEFAULT 'SYSTEM',

    correlation_id VARCHAR(128),
    tenant_id      VARCHAR(128),
    source_ip      VARCHAR(45),
    http_endpoint  VARCHAR(256),

    metadata       JSONB,

    occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_occurred_at ON audit_logs (occurred_at DESC);
CREATE INDEX idx_audit_action ON audit_logs (action);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_logs (actor_id);
CREATE INDEX idx_audit_correlation ON audit_logs (correlation_id);
CREATE INDEX idx_audit_tenant ON audit_logs (tenant_id);
CREATE INDEX idx_audit_success ON audit_logs (success);

CREATE INDEX idx_audit_action_actor_time ON audit_logs (action, actor_id, occurred_at DESC);

COMMENT ON TABLE audit_logs IS 'Trilha de auditoria append-only. Nunca atualizar ou deletar registros.';