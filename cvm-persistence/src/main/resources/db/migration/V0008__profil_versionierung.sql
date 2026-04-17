-- Iteration 04 -- Kontextprofil-Versionierung und NEEDS_REVIEW-Trigger.
--
-- Ergaenzt das Profil um die Status-Maschine (DRAFT/ACTIVE/SUPERSEDED) und
-- haengt Assessment um rationale_source_fields (JSONB-Array von
-- Profilpfaden) sowie den Review-Ausloeser an. Der Status NEEDS_REVIEW wird
-- in den Check-Constraint aufgenommen.

-- -- Context-Profile ------------------------------------------------------

ALTER TABLE context_profile
    ADD COLUMN IF NOT EXISTS state TEXT NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS proposed_by TEXT,
    ADD COLUMN IF NOT EXISTS superseded_at TIMESTAMPTZ;

ALTER TABLE context_profile
    DROP CONSTRAINT IF EXISTS ck_context_profile_state;
ALTER TABLE context_profile
    ADD CONSTRAINT ck_context_profile_state
    CHECK (state IN ('DRAFT','ACTIVE','SUPERSEDED'));

-- Hoechstens eine ACTIVE-Version je Umgebung.
DROP INDEX IF EXISTS idx_profile_active_per_env;
CREATE UNIQUE INDEX idx_profile_active_per_env
    ON context_profile (environment_id)
    WHERE state = 'ACTIVE';

-- -- Assessment -----------------------------------------------------------

ALTER TABLE assessment
    ADD COLUMN IF NOT EXISTS rationale_source_fields JSONB,
    ADD COLUMN IF NOT EXISTS review_triggered_by_profile_version UUID;

ALTER TABLE assessment
    DROP CONSTRAINT IF EXISTS ck_assessment_status;
ALTER TABLE assessment
    ADD CONSTRAINT ck_assessment_status
    CHECK (status IN ('PROPOSED','APPROVED','REJECTED','SUPERSEDED','NEEDS_REVIEW'));

-- GIN-Index auf die Pfadliste, damit das Review-Matching (jsonb ?|) fix
-- bleibt.
CREATE INDEX IF NOT EXISTS idx_assessment_rationale_source_fields
    ON assessment USING gin (rationale_source_fields);
