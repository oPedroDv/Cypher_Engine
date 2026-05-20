package com.cypher.audit.domain;

public enum AuditAction {

    ANALYSIS_REQUESTED         ("Análise de risco solicitada"),
    ANALYSIS_COMPLETED         ("Análise de risco concluída com sucesso"),
    ANALYSIS_FAILED            ("Análise de risco encerrada com falha"),
    ANALYSIS_REJECTED          ("Análise rejeitada por regra de negócio"),
    ANALYSIS_REPROCESSED       ("Análise reprocessada manualmente"),
    ANALYSIS_IDEMPOTENT_HIT    ("Requisição idempotente — análise já existente retornada"),

    INVOICE_RECEIVED           ("NF-e recebida para processamento"),
    INVOICE_VALIDATED          ("NF-e validada com sucesso"),
    INVOICE_VALIDATION_FAILED  ("NF-e rejeitada na validação de schema/assinatura"),
    INVOICE_DUPLICATE_DETECTED ("Tentativa de reprocessamento de NF-e duplicada"),
    INVOICE_STATUS_CHANGED     ("Status da NF-e atualizado"),
    INVOICE_XML_STORED         ("XML da NF-e armazenado no storage"),

    RISK_SCORE_CALCULATED      ("Score de risco calculado pelo engine"),
    RISK_RULE_APPLIED          ("Regra de risco aplicada à análise"),
    RISK_LEVEL_ELEVATED        ("Nível de risco elevado — requer revisão manual"),
    RISK_SCORE_OVERRIDDEN      ("Score de risco substituído por operador"),

    SEFAZ_STATUS_QUERIED       ("Status de autorização consultado na SEFAZ"),
    RECEITA_CNPJ_QUERIED       ("Situação cadastral consultada na Receita Federal"),

    COMPANY_HISTORY_UPDATED    ("Histórico de empresa atualizado"),

    AUTH_LOGIN_SUCCESS         ("Login realizado com sucesso"),
    AUTH_LOGIN_FAILURE         ("Tentativa de login falhou"),
    AUTH_LOGOUT                ("Sessão encerrada"),
    AUTH_TOKEN_ISSUED          ("Token de acesso emitido"),
    AUTH_TOKEN_REVOKED         ("Token de acesso revogado"),
    AUTH_PERMISSION_DENIED     ("Acesso negado por ausência de permissão"),

    SYSTEM_STARTUP             ("Aplicação iniciada"),
    SYSTEM_SHUTDOWN            ("Aplicação encerrada"),
    SYSTEM_CIRCUIT_BREAKER_OPEN("Circuit breaker aberto — serviço externo indisponível"),
    SYSTEM_CIRCUIT_BREAKER_CLOSE("Circuit breaker fechado — serviço externo recuperado"),
    SYSTEM_RATE_LIMIT_EXCEEDED ("Limite de requisições por cliente excedido"),

    DATA_EXPORT_REQUESTED      ("Exportação de dados solicitada"),
    DATA_DELETION_REQUESTED    ("Solicitação de exclusão de dados (LGPD Art. 18)"),
    DATA_SENSITIVE_ACCESSED    ("Acesso a campo sensível registrado"),
    DATA_RETENTION_PURGED      ("Registros expirados removidos por política de retenção");


    private final String description;

    AuditAction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}