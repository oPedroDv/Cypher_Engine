ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS tenant_id UUID,
    ADD COLUMN IF NOT EXISTS issuer_cnpj VARCHAR(14),
    ADD COLUMN IF NOT EXISTS issuer_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS recipient_cnpj VARCHAR(14),
    ADD COLUMN IF NOT EXISTS recipient_name VARCHAR(255);

ALTER TABLE risk_analysis
    ADD COLUMN IF NOT EXISTS tenant_id UUID;

UPDATE invoice
SET tenant_id = '00000000-0000-0000-0000-000000000001'
WHERE tenant_id IS NULL;

UPDATE risk_analysis ra
SET tenant_id = i.tenant_id
FROM invoice i
WHERE ra.invoice_id = i.id
  AND ra.tenant_id IS NULL;

UPDATE risk_analysis
SET tenant_id = '00000000-0000-0000-0000-000000000001'
WHERE tenant_id IS NULL;

ALTER TABLE invoice
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE risk_analysis
    ALTER COLUMN tenant_id SET NOT NULL;

DROP INDEX IF EXISTS idx_invoice_chave_nfe;
ALTER TABLE invoice
    DROP CONSTRAINT IF EXISTS invoice_chave_nfe_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_tenant_chave_nfe
    ON invoice (tenant_id, chave_nfe)
    WHERE chave_nfe IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_invoice_tenant_issuer
    ON invoice (tenant_id, issuer_cnpj);

CREATE INDEX IF NOT EXISTS idx_invoice_tenant_recipient
    ON invoice (tenant_id, recipient_cnpj);

DROP INDEX IF EXISTS idx_risk_analysis_created_at;
DROP INDEX IF EXISTS idx_risk_analysis_risk_level;

CREATE INDEX IF NOT EXISTS idx_risk_analysis_tenant_created_at
    ON risk_analysis (tenant_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_risk_analysis_tenant_risk_level
    ON risk_analysis (tenant_id, risk_level);

ALTER TABLE outcomes
    ADD CONSTRAINT fk_outcomes_analysis
    FOREIGN KEY (analysis_id)
    REFERENCES risk_analysis (id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_outcomes_tenant_analysis'
    ) THEN
        ALTER TABLE outcomes
            ADD CONSTRAINT uq_outcomes_tenant_analysis UNIQUE (tenant_id, analysis_id);
    END IF;
END $$;

DROP INDEX IF EXISTS idx_outcomes_tenant_id;
CREATE INDEX IF NOT EXISTS idx_outcomes_tenant_id
    ON outcomes (tenant_id);

CREATE INDEX IF NOT EXISTS idx_outcomes_tenant_event_date
    ON outcomes (tenant_id, event_date);
