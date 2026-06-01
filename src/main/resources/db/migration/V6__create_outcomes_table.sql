CREATE TABLE outcomes (
    id UUID PRIMARY KEY,
    analysis_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    outcome_type VARCHAR(50) NOT NULL,
    event_date DATE NOT NULL,
    amount_received NUMERIC(19, 2),
    days_late INTEGER,
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outcomes_analysis_id ON outcomes (analysis_id);
CREATE INDEX idx_outcomes_tenant_id ON outcomes (tenant_id);
CREATE INDEX idx_outcomes_event_date ON outcomes (event_date);

COMMENT ON TABLE outcomes IS 'Registro de desfecho de operações (pagamento, atraso, default).';
