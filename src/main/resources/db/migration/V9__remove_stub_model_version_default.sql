ALTER TABLE risk_analysis
    ALTER COLUMN model_version SET DEFAULT 'rule_engine_v1.0';

UPDATE risk_analysis
SET model_version = 'rule_engine_v1.0'
WHERE model_version = 'stub_v0.1';
claude
