-- Iteration 34 (CVM-78): LLM-Konfigurations-Verwaltung
--
-- Pro Mandant duerfen beliebig viele Konfigurationen existieren,
-- aber maximal eine ist aktiv. Das erzwingen wir unten ueber einen
-- partiellen UNIQUE-Index, damit die Regel auch bei paralleler
-- Anlage durchgesetzt wird.

CREATE TABLE IF NOT EXISTS llm_configuration (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    provider        VARCHAR(50) NOT NULL,
    model           VARCHAR(255) NOT NULL,
    base_url        VARCHAR(2048),
    secret_ref      TEXT,
    max_tokens      INT,
    temperature     NUMERIC(3,2),
    is_active       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by      VARCHAR(255),
    CONSTRAINT uq_llm_configuration_tenant_name
        UNIQUE (tenant_id, name),
    CONSTRAINT ck_llm_configuration_temperature
        CHECK (temperature IS NULL OR (temperature >= 0 AND temperature <= 1)),
    CONSTRAINT ck_llm_configuration_max_tokens
        CHECK (max_tokens IS NULL OR max_tokens > 0)
);

-- Partieller Unique-Index: nur EINE aktive Konfiguration pro Tenant.
-- Beim Setzen is_active=TRUE muss der Service zuerst die bisher
-- aktive deaktivieren; wird das vergessen, greift hier die DB.
CREATE UNIQUE INDEX IF NOT EXISTS ux_llm_configuration_active_per_tenant
    ON llm_configuration (tenant_id)
    WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS ix_llm_configuration_tenant
    ON llm_configuration (tenant_id);
