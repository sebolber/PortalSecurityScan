-- Iteration 11 -- LLM-Gateway + Audit (CVM-30).
--
-- Drei Tabellen:
--   ai_call_audit      pro KI-Call ein Datensatz, zweistufig (PENDING -> OK/...)
--   ai_suggestion      strukturierter Vorschlag (ein Call kann mehrere
--                      Vorschlaege produzieren, derzeit typisch 1).
--   ai_source_ref      Begruendungsquellen (Profilpfade, CVE-Referenzen,
--                      Doku-Schnipsel) fuer Nachvollziehbarkeit.
--
-- ai_call_audit ist fachlich unveraenderlich, ein Update ist nur zur
-- Finalisierung vom PENDING-Status erlaubt. Eine Application-Side
-- Immutability-Kontrolle (JPA-Listener in cvm-persistence) schuetzt
-- alle anderen Felder. Harte DB-Garantie folgt ueber einen spaeteren
-- Trigger (offen).

CREATE TABLE IF NOT EXISTS ai_call_audit (
    id                       UUID PRIMARY KEY,
    use_case                 TEXT NOT NULL,
    model_id                 TEXT NOT NULL,
    model_version            TEXT,
    prompt_template_id       TEXT NOT NULL,
    prompt_template_version  TEXT NOT NULL,
    system_prompt            TEXT NOT NULL,
    user_prompt              TEXT NOT NULL,
    rag_context              TEXT,
    raw_response             TEXT,
    prompt_tokens            INTEGER,
    completion_tokens        INTEGER,
    latency_ms               INTEGER,
    cost_eur                 NUMERIC(10,6),
    triggered_by             TEXT NOT NULL,
    environment_id           UUID REFERENCES environment(id),
    status                   TEXT NOT NULL,
    injection_risk           BOOLEAN NOT NULL DEFAULT FALSE,
    invalid_output_reason    TEXT,
    error_message            TEXT,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    finalized_at             TIMESTAMPTZ,
    CONSTRAINT ck_ai_call_audit_status
        CHECK (status IN ('PENDING','OK','INVALID_OUTPUT',
                          'INJECTION_RISK','ERROR','RATE_LIMITED','DISABLED'))
);

CREATE INDEX IF NOT EXISTS idx_ai_call_audit_created
    ON ai_call_audit (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_call_audit_status
    ON ai_call_audit (status);
CREATE INDEX IF NOT EXISTS idx_ai_call_audit_use_case
    ON ai_call_audit (use_case, created_at DESC);

CREATE TABLE IF NOT EXISTS ai_suggestion (
    id                UUID PRIMARY KEY,
    ai_call_audit_id  UUID NOT NULL REFERENCES ai_call_audit(id) ON DELETE CASCADE,
    use_case          TEXT NOT NULL,
    finding_id        UUID REFERENCES finding(id),
    cve_id            UUID REFERENCES cve(id),
    environment_id    UUID REFERENCES environment(id),
    severity          TEXT,
    rationale         TEXT,
    confidence        NUMERIC(4,3),
    status            TEXT NOT NULL DEFAULT 'PROPOSED',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_ai_suggestion_severity
        CHECK (severity IS NULL OR severity IN (
            'CRITICAL','HIGH','MEDIUM','LOW','INFORMATIONAL','NOT_APPLICABLE')),
    CONSTRAINT ck_ai_suggestion_status
        CHECK (status IN ('PROPOSED','ACCEPTED','REJECTED'))
);

CREATE INDEX IF NOT EXISTS idx_ai_suggestion_call
    ON ai_suggestion (ai_call_audit_id);
CREATE INDEX IF NOT EXISTS idx_ai_suggestion_finding
    ON ai_suggestion (finding_id);

CREATE TABLE IF NOT EXISTS ai_source_ref (
    id                UUID PRIMARY KEY,
    ai_suggestion_id  UUID NOT NULL REFERENCES ai_suggestion(id) ON DELETE CASCADE,
    kind              TEXT NOT NULL,
    reference         TEXT NOT NULL,
    excerpt           TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_ai_source_ref_kind
        CHECK (kind IN ('PROFILE_PATH','CVE','RULE','DOCUMENT','CODE_REF'))
);

CREATE INDEX IF NOT EXISTS idx_ai_source_ref_suggestion
    ON ai_source_ref (ai_suggestion_id);
