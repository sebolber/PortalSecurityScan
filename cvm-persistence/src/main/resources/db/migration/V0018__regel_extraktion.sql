-- Iteration 17 -- KI-Regel-Extraktion (CVM-42).
--
-- Speichert extrahierte Regel-Vorschlaege + Dry-Run-Metriken +
-- Konflikt-Liste. Aktiviert werden sie nur ueber einen Approve-Call
-- (Vier-Augen); dabei entsteht ein regulaerer rule-Eintrag mit
-- extracted_from_ai_suggestion_id.

CREATE TABLE IF NOT EXISTS rule_suggestion (
    id                          UUID PRIMARY KEY,
    ai_suggestion_id            UUID NOT NULL REFERENCES ai_suggestion(id)
                                    ON DELETE CASCADE,
    name                        TEXT NOT NULL,
    condition_json              TEXT NOT NULL,
    proposed_severity           TEXT NOT NULL,
    rationale_template          TEXT NOT NULL,
    cluster_rationale           TEXT,
    historical_match_count      INTEGER NOT NULL DEFAULT 0,
    would_have_covered          INTEGER NOT NULL DEFAULT 0,
    coverage_rate               NUMERIC(4,3) NOT NULL DEFAULT 0,
    conflict_count              INTEGER NOT NULL DEFAULT 0,
    status                      TEXT NOT NULL DEFAULT 'PROPOSED',
    suggested_by                TEXT NOT NULL,
    approved_by                 TEXT,
    approved_at                 TIMESTAMPTZ,
    rejected_by                 TEXT,
    rejected_at                 TIMESTAMPTZ,
    reject_comment              TEXT,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ,
    CONSTRAINT ck_rule_suggestion_status
        CHECK (status IN ('PROPOSED','APPROVED','REJECTED','EXPIRED')),
    CONSTRAINT ck_rule_suggestion_severity
        CHECK (proposed_severity IN (
            'CRITICAL','HIGH','MEDIUM','LOW','INFORMATIONAL','NOT_APPLICABLE'))
);

CREATE INDEX IF NOT EXISTS idx_rule_suggestion_status
    ON rule_suggestion (status, created_at DESC);

CREATE TABLE IF NOT EXISTS rule_suggestion_example (
    id                  UUID PRIMARY KEY,
    rule_suggestion_id  UUID NOT NULL REFERENCES rule_suggestion(id) ON DELETE CASCADE,
    assessment_id       UUID NOT NULL REFERENCES assessment(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_rule_suggestion_example UNIQUE (rule_suggestion_id, assessment_id)
);

CREATE TABLE IF NOT EXISTS rule_suggestion_conflict (
    id                  UUID PRIMARY KEY,
    rule_suggestion_id  UUID NOT NULL REFERENCES rule_suggestion(id) ON DELETE CASCADE,
    assessment_id       UUID NOT NULL REFERENCES assessment(id),
    actual_severity     TEXT,
    note                TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_rule_suggestion_conflict UNIQUE (rule_suggestion_id, assessment_id)
);

-- rule bekommt den Verweis auf den Vorschlag. NULL = manuell angelegt.
ALTER TABLE rule
    ADD COLUMN IF NOT EXISTS extracted_from_ai_suggestion_id UUID
        REFERENCES ai_suggestion(id);
