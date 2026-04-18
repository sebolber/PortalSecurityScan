-- Iteration 18 -- KI-Anomalie-Check + Profil-Assistent (CVM-43).

CREATE TABLE IF NOT EXISTS anomaly_event (
    id              UUID PRIMARY KEY,
    assessment_id   UUID NOT NULL REFERENCES assessment(id),
    pattern         TEXT NOT NULL,
    severity        TEXT NOT NULL,
    reason          TEXT NOT NULL,
    pointers_json   TEXT,
    triggered_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_anomaly_event_severity
        CHECK (severity IN ('INFO','WARNING','CRITICAL')),
    CONSTRAINT ck_anomaly_event_pattern
        CHECK (pattern IN (
            'KEV_NOT_APPLICABLE',
            'MANY_ACCEPT_RISK',
            'SIMILAR_TO_REJECTED',
            'BIG_DOWNGRADE_WITHOUT_RULE'))
);

CREATE INDEX IF NOT EXISTS idx_anomaly_event_trig
    ON anomaly_event (triggered_at DESC);
CREATE INDEX IF NOT EXISTS idx_anomaly_event_assessment
    ON anomaly_event (assessment_id);

-- Alert-Trigger fuer KI-Anomalie: Erweitert den bestehenden
-- ck_alert_rule_trigger-Constraint um 'KI_ANOMALIE'.
ALTER TABLE alert_rule
    DROP CONSTRAINT IF EXISTS ck_alert_rule_trigger;
ALTER TABLE alert_rule
    ADD CONSTRAINT ck_alert_rule_trigger
    CHECK (trigger_art IN (
        'FINDING_NEU','ASSESSMENT_PROPOSED','ASSESSMENT_APPROVED',
        'ESKALATION_T1','ESKALATION_T2','KEV_HIT','KI_ANOMALIE'));

-- Profil-Assistent: stateful Session-Tabelle mit Dialog-Historie.
CREATE TABLE IF NOT EXISTS profile_assist_session (
    id                   UUID PRIMARY KEY,
    environment_id       UUID NOT NULL REFERENCES environment(id),
    started_by           TEXT NOT NULL,
    dialog_json          TEXT NOT NULL DEFAULT '[]',
    pending_question     TEXT,
    status               TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ,
    expires_at           TIMESTAMPTZ NOT NULL,
    finalized_draft_id   UUID REFERENCES context_profile(id),
    CONSTRAINT ck_profile_assist_status
        CHECK (status IN ('ACTIVE','FINALIZED','EXPIRED','CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_profile_assist_env
    ON profile_assist_session (environment_id, created_at DESC);
