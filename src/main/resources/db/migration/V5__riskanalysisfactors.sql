ALTER TABLE risk_analysis
    ADD COLUMN IF NOT EXISTS factors           JSONB,
    ADD COLUMN IF NOT EXISTS financial_metrics JSONB,
    ADD COLUMN IF NOT EXISTS data_partial      BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_risk_analysis_invoice_id ON risk_analysis (invoice_id);
CREATE INDEX IF NOT EXISTS idx_risk_analysis_created_at ON risk_analysis (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_risk_analysis_risk_level ON risk_analysis (risk_level);

COMMENT ON COLUMN risk_analysis.factors IS 'Snapshot dos RiskFactors no momento da análise (JSONB).';
COMMENT ON COLUMN risk_analysis.financial_metrics IS 'Snapshot das FinancialMetrics no momento da análise (JSONB).';
COMMENT ON COLUMN risk_analysis.data_partial IS 'TRUE se alguma fonte estava indisponível e fallbacks foram aplicados.';