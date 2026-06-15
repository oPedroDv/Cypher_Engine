CREATE EXTENSION IF NOT EXISTS "pgcrypto";

ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS chave_nfe  VARCHAR(44) UNIQUE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

ALTER TABLE risk_analysis
    ADD COLUMN IF NOT EXISTS invoice_id    UUID REFERENCES invoice (id),
    ADD COLUMN IF NOT EXISTS model_version VARCHAR(50)              NOT NULL DEFAULT 'stub_v0.1',
    ADD COLUMN IF NOT EXISTS created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW();

ALTER TABLE risk_analysis
    ALTER COLUMN invoice_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_invoice_chave_nfe
    ON invoice (chave_nfe)
    WHERE chave_nfe IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_risk_analysis_invoice_id
    ON risk_analysis (invoice_id);

CREATE INDEX IF NOT EXISTS idx_risk_analysis_created_at
    ON risk_analysis (created_at DESC);
