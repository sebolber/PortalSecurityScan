-- Iteration 31 (CVM-72): Branding-Historie fuer One-Click-Rollback.
--
-- Jede erfolgreiche Aenderung an `branding_config` fuehrt zu einem
-- Snapshot des ALTEN Standes in dieser Tabelle. Damit ist sowohl
-- ein One-Click-Rollback moeglich als auch eine Audit-Liste pro
-- Mandant.

CREATE TABLE IF NOT EXISTS branding_config_history (
    history_id             UUID PRIMARY KEY,
    tenant_id              UUID NOT NULL,
    primary_color          TEXT NOT NULL,
    primary_contrast_color TEXT NOT NULL,
    accent_color           TEXT,
    font_family_name       TEXT NOT NULL,
    font_family_mono_name  TEXT,
    app_title              TEXT,
    logo_url               TEXT,
    logo_alt_text          TEXT,
    favicon_url            TEXT,
    font_family_href       TEXT,
    version                INT NOT NULL,
    updated_at             TIMESTAMPTZ NOT NULL,
    updated_by             TEXT NOT NULL,
    recorded_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    recorded_by            TEXT NOT NULL,
    CONSTRAINT uq_branding_history_tenant_version
        UNIQUE (tenant_id, version)
);

CREATE INDEX IF NOT EXISTS idx_branding_history_tenant
    ON branding_config_history (tenant_id, version DESC);
