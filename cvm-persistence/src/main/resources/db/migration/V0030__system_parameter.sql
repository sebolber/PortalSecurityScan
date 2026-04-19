-- Iteration: Systemparameter-Verwaltung (Vorlage: PortalCore Parameter-Dialog)
--
-- Mandantenspezifische Key/Value-Konfiguration mit Typ, Validierung,
-- Default-Wert, Sichtbarkeit, Hot-Reload-Hinweis und Audit-Log.
--
-- Tenant-Isolation: tenant_id ist Pflicht. Der Schluessel (param_key)
-- ist innerhalb eines Mandanten eindeutig. Das Audit-Log haelt jede
-- Wertaenderung mit altem/neuem Wert und Begruendung fest.
--
-- Sensitive Parameter (z.B. Tokens) werden im UI maskiert dargestellt;
-- die Maskierung erfolgt im Application-Service vor der Auslieferung
-- und vor dem Schreiben des Audit-Eintrags.

CREATE TABLE IF NOT EXISTS system_parameter (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    param_key       VARCHAR(255) NOT NULL,
    label           VARCHAR(255) NOT NULL,
    description     TEXT,
    handbook        TEXT,
    category        VARCHAR(100) NOT NULL,
    subcategory     VARCHAR(100),
    type            VARCHAR(32)  NOT NULL,
    value           TEXT,
    default_value   TEXT,
    required        BOOLEAN      NOT NULL DEFAULT FALSE,
    validation_rules TEXT,
    options         TEXT,
    unit            VARCHAR(64),
    sensitive       BOOLEAN      NOT NULL DEFAULT FALSE,
    hot_reload      BOOLEAN      NOT NULL DEFAULT FALSE,
    valid_from      TIMESTAMPTZ,
    valid_to        TIMESTAMPTZ,
    admin_only      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      VARCHAR(255),
    CONSTRAINT uq_system_parameter_tenant_key UNIQUE (tenant_id, param_key),
    CONSTRAINT ck_system_parameter_type CHECK (type IN (
        'STRING','INTEGER','DECIMAL','BOOLEAN','EMAIL','URL','JSON',
        'PASSWORD','SELECT','MULTISELECT','DATE','TIMESTAMP','TEXTAREA',
        'HOST','IP'
    )),
    CONSTRAINT ck_system_parameter_validity
        CHECK (valid_from IS NULL OR valid_to IS NULL OR valid_from <= valid_to)
);

CREATE INDEX IF NOT EXISTS ix_system_parameter_tenant
    ON system_parameter (tenant_id);
CREATE INDEX IF NOT EXISTS ix_system_parameter_category
    ON system_parameter (tenant_id, category);


CREATE TABLE IF NOT EXISTS system_parameter_audit_log (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    parameter_id    UUID NOT NULL,
    param_key       VARCHAR(255) NOT NULL,
    old_value       TEXT,
    new_value       TEXT,
    changed_by      VARCHAR(255) NOT NULL,
    changed_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    reason          TEXT,
    CONSTRAINT fk_system_parameter_audit_param
        FOREIGN KEY (parameter_id) REFERENCES system_parameter(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_system_parameter_audit_tenant
    ON system_parameter_audit_log (tenant_id, changed_at DESC);
CREATE INDEX IF NOT EXISTS ix_system_parameter_audit_param
    ON system_parameter_audit_log (parameter_id, changed_at DESC);
