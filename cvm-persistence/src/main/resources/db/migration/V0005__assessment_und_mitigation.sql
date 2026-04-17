-- Assessments sind immutable-versioniert: eine Aenderung resultiert in einer
-- neuen Zeile mit version = previous.version + 1. Die alte Zeile wird nicht
-- physisch geloescht, sondern via superseded_at und Status SUPERSEDED entwertet.
--
-- Der Spaltenplatzhalter ai_suggestion_id bleibt nullable und ohne FK,
-- damit Iteration 11 nur noch die Ziel-Tabelle (ai_suggestion) ergaenzt und
-- einen FK nachruestet.

CREATE TABLE assessment (
    id UUID PRIMARY KEY,
    finding_id UUID NOT NULL REFERENCES finding (id) ON DELETE CASCADE,
    product_version_id UUID NOT NULL REFERENCES product_version (id) ON DELETE CASCADE,
    environment_id UUID NOT NULL REFERENCES environment (id) ON DELETE CASCADE,
    cve_id UUID NOT NULL REFERENCES cve (id) ON DELETE RESTRICT,
    version INTEGER NOT NULL,
    status TEXT NOT NULL,
    severity TEXT NOT NULL,
    proposal_source TEXT NOT NULL,
    rationale TEXT,
    decided_by TEXT,
    decided_at TIMESTAMPTZ,
    superseded_at TIMESTAMPTZ,
    ai_suggestion_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_assessment_finding_version UNIQUE (finding_id, version),
    CONSTRAINT ck_assessment_status
        CHECK (status IN ('PROPOSED','APPROVED','REJECTED','SUPERSEDED')),
    CONSTRAINT ck_assessment_severity
        CHECK (severity IN ('CRITICAL','HIGH','MEDIUM','LOW','INFORMATIONAL','NOT_APPLICABLE')),
    CONSTRAINT ck_assessment_source
        CHECK (proposal_source IN ('REUSE','RULE','AI_SUGGESTION','HUMAN'))
);

CREATE INDEX idx_assessment_finding ON assessment (finding_id);
CREATE INDEX idx_assessment_cve_env ON assessment (cve_id, environment_id);
CREATE INDEX idx_assessment_superseded ON assessment (superseded_at) WHERE superseded_at IS NULL;

CREATE TABLE mitigation_plan (
    id UUID PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES assessment (id) ON DELETE CASCADE,
    strategy TEXT NOT NULL,
    status TEXT NOT NULL,
    target_version TEXT,
    owner TEXT,
    planned_for TIMESTAMPTZ,
    implemented_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT ck_mitigation_strategy CHECK (strategy IN (
        'UPGRADE','PATCH','CONFIG_CHANGE','WORKAROUND','ACCEPT_RISK','NOT_APPLICABLE')),
    CONSTRAINT ck_mitigation_status CHECK (status IN (
        'OPEN','PLANNED','IN_PROGRESS','IMPLEMENTED','VERIFIED','WAIVED'))
);

CREATE INDEX idx_mitigation_assessment ON mitigation_plan (assessment_id);
