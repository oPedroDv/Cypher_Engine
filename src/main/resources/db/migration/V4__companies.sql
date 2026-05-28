CREATE TABLE companies
(
    id                   UUID         NOT NULL,
    cnpj                 VARCHAR(14)  NOT NULL,
    razao_social         VARCHAR(256) NOT NULL,
    nome_fantasia        VARCHAR(256),
    cnpj_status          VARCHAR(32)  NOT NULL DEFAULT 'UNKNOWN',
    status_checked_at    TIMESTAMPTZ,
    receita_federal_data JSONB,
    tenant_id            UUID         NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_companies PRIMARY KEY (id),
    CONSTRAINT uq_companies_cnpj_tenant UNIQUE (cnpj, tenant_id),
    CONSTRAINT chk_companies_cnpj_length CHECK (LENGTH(cnpj) = 14),
    CONSTRAINT chk_companies_cnpj_status CHECK (
        cnpj_status IN ('ACTIVE', 'SUSPENDED', 'UNFIT', 'CLOSED', 'NULLIFIED', 'UNKNOWN')
        )
);

CREATE INDEX idx_companies_cnpj ON companies (cnpj);
CREATE INDEX idx_companies_tenant_id ON companies (tenant_id);
CREATE INDEX idx_companies_cnpj_status ON companies (cnpj_status);

CREATE INDEX idx_companies_status_checked ON companies (status_checked_at ASC NULLS FIRST)
    WHERE status_checked_at IS NULL OR cnpj_status != 'CLOSED';

COMMENT ON TABLE companies IS 'Empresas cadastradas por tenant. CNPJ único por tenant.';
COMMENT ON COLUMN companies.receita_federal_data IS 'Dados brutos retornados pela Receita Federal em JSONB.';
COMMENT ON COLUMN companies.status_checked_at IS 'Última sincronização com a Receita Federal. NULL = nunca consultado.';
