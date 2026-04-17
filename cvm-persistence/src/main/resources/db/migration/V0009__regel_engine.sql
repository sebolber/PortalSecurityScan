-- Iteration 05 -- Regel-Engine.
--
-- Regeln sind Vier-Augen-freigegeben. Jede Regel traegt ihre Condition als
-- JSONB, ein Rationale-Template und die vorgeschlagene Severity. Dry-Run-
-- Laeufe werden in rule_dry_run_result persistiert, damit die UI spaeter
-- (Iteration 08) Coverage-Verlauf und Konflikte zeigen kann.

CREATE TABLE rule (
    id UUID PRIMARY KEY,
    rule_key TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    origin TEXT NOT NULL,
    status TEXT NOT NULL,
    proposed_severity TEXT NOT NULL,
    condition_json TEXT NOT NULL,
    rationale_template TEXT NOT NULL,
    rationale_source_fields JSONB,
    created_by TEXT NOT NULL,
    activated_by TEXT,
    activated_at TIMESTAMPTZ,
    retired_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_rule_key UNIQUE (rule_key),
    CONSTRAINT ck_rule_origin CHECK (origin IN ('MANUAL','AI_EXTRACTED')),
    CONSTRAINT ck_rule_status CHECK (status IN ('DRAFT','ACTIVE','RETIRED')),
    CONSTRAINT ck_rule_severity CHECK (proposed_severity IN (
        'CRITICAL','HIGH','MEDIUM','LOW','INFORMATIONAL','NOT_APPLICABLE'))
);

CREATE INDEX idx_rule_status ON rule (status);

CREATE TABLE rule_dry_run_result (
    id UUID PRIMARY KEY,
    rule_id UUID NOT NULL REFERENCES rule (id) ON DELETE CASCADE,
    executed_at TIMESTAMPTZ NOT NULL,
    range_start TIMESTAMPTZ NOT NULL,
    range_end TIMESTAMPTZ NOT NULL,
    total_findings INTEGER NOT NULL,
    matched_findings INTEGER NOT NULL,
    matched_already_approved INTEGER NOT NULL,
    conflicts JSONB,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_rule_dry_run_rule ON rule_dry_run_result (rule_id);
CREATE INDEX idx_rule_dry_run_executed ON rule_dry_run_result (executed_at DESC);
