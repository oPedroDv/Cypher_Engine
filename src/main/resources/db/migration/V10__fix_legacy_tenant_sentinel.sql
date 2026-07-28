
ALTER TABLE IF EXISTS invoice
    ADD COLUMN IF NOT EXISTS legacy_migrated BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE IF EXISTS risk_analysis
    ADD COLUMN IF NOT EXISTS legacy_migrated BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE invoice
SET legacy_migrated = TRUE
WHERE tenant_id = '00000000-0000-0000-0000-000000000001'
  AND legacy_migrated = FALSE;

UPDATE risk_analysis
SET legacy_migrated = TRUE
WHERE tenant_id = '00000000-0000-0000-0000-000000000001'
  AND legacy_migrated = FALSE;

COMMENT ON COLUMN invoice.legacy_migrated IS
    'TRUE when V8 assigned the legacy sentinel tenant; ownership requires manual review.';
COMMENT ON COLUMN risk_analysis.legacy_migrated IS
    'TRUE when V8 assigned the legacy sentinel tenant; ownership requires manual review.';
