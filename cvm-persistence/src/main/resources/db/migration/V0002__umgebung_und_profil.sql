-- Umgebungen (Dev/Test/Ref/Abn/Prod) und Kontextprofile.
-- ContextProfile ist versioniert: je Umgebung koennen mehrere Profile existieren,
-- das jeweils ab valid_from geltende ist aktiv.

CREATE TABLE environment (
    id UUID PRIMARY KEY,
    key TEXT NOT NULL,
    name TEXT NOT NULL,
    stage TEXT NOT NULL,
    tenant TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_environment_key UNIQUE (key),
    CONSTRAINT ck_environment_stage CHECK (stage IN ('DEV','TEST','REF','ABN','PROD'))
);

CREATE TABLE context_profile (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL REFERENCES environment (id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    yaml_source TEXT NOT NULL,
    approved_by TEXT,
    approved_at TIMESTAMPTZ,
    needs_review BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_profile_version UNIQUE (environment_id, version_number)
);

CREATE INDEX idx_context_profile_env_valid
    ON context_profile (environment_id, valid_from DESC);

CREATE TABLE environment_deployment (
    id UUID PRIMARY KEY,
    environment_id UUID NOT NULL REFERENCES environment (id) ON DELETE CASCADE,
    product_version_id UUID NOT NULL REFERENCES product_version (id) ON DELETE CASCADE,
    deployed_at TIMESTAMPTZ NOT NULL,
    retired_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_env_deployment_env ON environment_deployment (environment_id);
CREATE INDEX idx_env_deployment_pv ON environment_deployment (product_version_id);
