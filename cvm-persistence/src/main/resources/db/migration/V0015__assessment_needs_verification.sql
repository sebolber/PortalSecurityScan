-- Iteration 13 -- KI-Vorbewertung (CVM-32).
--
-- Erweitert den Assessment-Status-Check um NEEDS_VERIFICATION.
-- Der neue Status entsteht aus dem KI-Cascade, wenn der
-- Halluzinations-Check eine vom LLM gelieferte Faktenangabe nicht
-- belegen konnte.

ALTER TABLE assessment
    DROP CONSTRAINT IF EXISTS ck_assessment_status;
ALTER TABLE assessment
    ADD CONSTRAINT ck_assessment_status
    CHECK (status IN (
        'PROPOSED','APPROVED','REJECTED','SUPERSEDED',
        'NEEDS_REVIEW','NEEDS_VERIFICATION','EXPIRED'));

-- Bewertungs-Queue zeigt auch NEEDS_VERIFICATION-Eintraege.
DROP INDEX IF EXISTS idx_assessment_queue_offen;
CREATE INDEX IF NOT EXISTS idx_assessment_queue_offen
    ON assessment (environment_id, severity, created_at)
    WHERE superseded_at IS NULL
      AND status IN ('PROPOSED','NEEDS_REVIEW','NEEDS_VERIFICATION');
