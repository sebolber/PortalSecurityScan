-- Iteration 27 (CVM-61): Mandantenspezifisches Branding / Theming.
--
-- Eine Zeile pro Mandant. Asset-Tabelle (Logo, Favicon, Fonts) ist
-- im Schema bereits angelegt, der Upload-Endpoint folgt in
-- Iteration 27b (siehe docs/20260418/iteration-27-plan.md).
-- Bis dahin werden Logo- und Favicon-URLs in `*_url`-Feldern als
-- externe URLs gepflegt.

CREATE TABLE IF NOT EXISTS branding_config (
    tenant_id              UUID PRIMARY KEY,
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
    version                INT NOT NULL DEFAULT 1,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by             TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS branding_asset (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    kind          TEXT NOT NULL,       -- LOGO | FAVICON | FONT
    content_type  TEXT NOT NULL,
    size_bytes    INT NOT NULL,
    sha256        TEXT NOT NULL,
    bytes         BYTEA NOT NULL,
    uploaded_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    uploaded_by   TEXT NOT NULL,
    CONSTRAINT ck_branding_asset_kind
        CHECK (kind IN ('LOGO','FAVICON','FONT'))
);

CREATE INDEX IF NOT EXISTS idx_branding_asset_tenant
    ON branding_asset (tenant_id, kind);

-- Default-Branding fuer den Default-Mandanten anlegen. adesso-CI
-- (Blau #006ec7, Fira Sans) gemaess Styleguide Maerz 2026.
INSERT INTO branding_config (
    tenant_id, primary_color, primary_contrast_color, accent_color,
    font_family_name, font_family_mono_name, app_title, logo_alt_text,
    version, updated_by
)
SELECT
    t.id, '#006ec7', '#ffffff', '#887d75',
    'Fira Sans', 'Fira Code', 'CVE-Relevance-Manager',
    'adesso health solutions',
    1, 'system'
FROM tenant t
WHERE t.is_default = TRUE
ON CONFLICT (tenant_id) DO NOTHING;
