-- Iteration 09 -- SMTP-Alerts.
--
-- Drei Tabellen:
--   alert_rule      konfigurierte Regeln (Name, Trigger-Art, Severity,
--                   Cooldown, Empfaenger, optional Bedingungs-JSON).
--   alert_event     Cooldown-Buchhaltung (rule_id + trigger_key).
--   alert_dispatch  Audit-Log fuer tatsaechlich versandte Mails.
--
-- Vorwaerts-Migration. Alle UUIDs werden vom Service erzeugt.

CREATE TABLE IF NOT EXISTS alert_rule (
    id                 UUID PRIMARY KEY,
    name               TEXT NOT NULL,
    description        TEXT,
    trigger_art        TEXT NOT NULL,
    severity           TEXT NOT NULL,
    cooldown_minutes   INTEGER NOT NULL DEFAULT 60,
    subject_prefix     TEXT NOT NULL DEFAULT '[CVM]',
    template_name      TEXT NOT NULL,
    recipients         JSONB NOT NULL,
    condition_json     TEXT,
    enabled            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ,
    CONSTRAINT ck_alert_rule_severity
        CHECK (severity IN ('INFO','WARNING','CRITICAL')),
    CONSTRAINT ck_alert_rule_trigger
        CHECK (trigger_art IN (
            'FINDING_NEU','ASSESSMENT_PROPOSED','ASSESSMENT_APPROVED',
            'ESKALATION_T1','ESKALATION_T2','KEV_HIT'))
);

CREATE INDEX IF NOT EXISTS idx_alert_rule_enabled
    ON alert_rule (enabled, trigger_art);

CREATE TABLE IF NOT EXISTS alert_event (
    id              UUID PRIMARY KEY,
    rule_id         UUID NOT NULL REFERENCES alert_rule(id) ON DELETE CASCADE,
    trigger_key     TEXT NOT NULL,
    last_fired_at   TIMESTAMPTZ NOT NULL,
    suppressed_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uq_alert_event_rule_key UNIQUE (rule_id, trigger_key)
);

CREATE INDEX IF NOT EXISTS idx_alert_event_last_fired
    ON alert_event (last_fired_at);

CREATE TABLE IF NOT EXISTS alert_dispatch (
    id            UUID PRIMARY KEY,
    rule_id       UUID NOT NULL REFERENCES alert_rule(id) ON DELETE CASCADE,
    trigger_key   TEXT NOT NULL,
    dispatched_at TIMESTAMPTZ NOT NULL,
    recipients    JSONB NOT NULL,
    subject       TEXT NOT NULL,
    body_excerpt  TEXT,
    dry_run       BOOLEAN NOT NULL DEFAULT FALSE,
    error         TEXT
);

CREATE INDEX IF NOT EXISTS idx_alert_dispatch_rule
    ON alert_dispatch (rule_id, dispatched_at DESC);
