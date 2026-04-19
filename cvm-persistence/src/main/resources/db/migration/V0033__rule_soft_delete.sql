-- Iteration 50 (CVM-100): Regel-Soft-Delete.
--
-- Abgrenzung zum Status RETIRED:
-- - RETIRED = fachlich abgeloest (eine neue Regel hat sie ersetzt);
--   der Eintrag steht historisch im Audit-Trail und hat bewusst ein
--   retired_at.
-- - deleted_at != NULL = technisch entfernt; die Regel verschwindet
--   aus Admin-Listen UND aus der Regel-Engine-Auswertung, bleibt aber
--   in der Tabelle stehen, damit historische Assessments, die sich
--   auf sie beziehen, ihre Rationale weiter aufloesen koennen.

ALTER TABLE rule
    ADD COLUMN deleted_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_rule_deleted_at
    ON rule (deleted_at)
    WHERE deleted_at IS NOT NULL;
