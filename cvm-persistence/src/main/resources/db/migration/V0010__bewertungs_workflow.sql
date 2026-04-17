-- Iteration 06 -- Bewertungs-Workflow.
--
-- Ergaenzt das Assessment um valid_until (Ablaufdatum, wird vom Scheduler
-- ausgewertet) und reviewed_by (Zweitbewerter bei Vier-Augen-Downgrades).
-- Der Check-Constraint fuer den Status-Zyklus wird um EXPIRED erweitert.

ALTER TABLE assessment
    ADD COLUMN IF NOT EXISTS valid_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reviewed_by TEXT;

ALTER TABLE assessment
    DROP CONSTRAINT IF EXISTS ck_assessment_status;
ALTER TABLE assessment
    ADD CONSTRAINT ck_assessment_status
    CHECK (status IN (
        'PROPOSED','APPROVED','REJECTED','SUPERSEDED','NEEDS_REVIEW','EXPIRED'));

-- Partial-Index fuer die Bewertungs-Queue: aktive offene Vorschlaege.
CREATE INDEX IF NOT EXISTS idx_assessment_queue_offen
    ON assessment (environment_id, severity, created_at)
    WHERE superseded_at IS NULL
      AND status IN ('PROPOSED','NEEDS_REVIEW');

-- Expiry-Scheduler findet APPROVED-Assessments mit abgelaufenem validUntil.
CREATE INDEX IF NOT EXISTS idx_assessment_expiry
    ON assessment (valid_until)
    WHERE status = 'APPROVED' AND superseded_at IS NULL;
