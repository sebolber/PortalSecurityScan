-- Iteration 22 (CVM-53): KPI-Tages-Snapshot.
--
-- Persistiert die offenen CVE-Counts je Severity + Automatisierungs-
-- quote pro (productVersionId, environmentId) am Tag des Laufs.
-- Ersetzt perspektivisch die teure Stream-Aggregation in KpiService
-- fuer historische Burn-Down-Kurven.

CREATE TABLE IF NOT EXISTS kpi_snapshot_daily (
    id                    UUID PRIMARY KEY,
    snapshot_day          DATE NOT NULL,
    product_version_id    UUID,
    environment_id        UUID,
    open_critical         INTEGER NOT NULL DEFAULT 0,
    open_high             INTEGER NOT NULL DEFAULT 0,
    open_medium           INTEGER NOT NULL DEFAULT 0,
    open_low              INTEGER NOT NULL DEFAULT 0,
    open_informational    INTEGER NOT NULL DEFAULT 0,
    automation_rate       NUMERIC(5,4) NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Genau ein Snapshot pro Tag und Scope. NULL-Scope (Global-Snapshot)
-- ist erlaubt und unique via partieller Indexe.
CREATE UNIQUE INDEX IF NOT EXISTS ux_kpi_snapshot_daily_scoped
    ON kpi_snapshot_daily (snapshot_day, product_version_id, environment_id)
    WHERE product_version_id IS NOT NULL AND environment_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_kpi_snapshot_daily_pv_nur
    ON kpi_snapshot_daily (snapshot_day, product_version_id)
    WHERE product_version_id IS NOT NULL AND environment_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_kpi_snapshot_daily_env_nur
    ON kpi_snapshot_daily (snapshot_day, environment_id)
    WHERE product_version_id IS NULL AND environment_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_kpi_snapshot_daily_global
    ON kpi_snapshot_daily (snapshot_day)
    WHERE product_version_id IS NULL AND environment_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_kpi_snapshot_daily_day
    ON kpi_snapshot_daily (snapshot_day DESC);
