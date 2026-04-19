-- Iteration 51 (CVM-101): Kontextprofil-Soft-Delete + Edit.
-- Bestehende Profil-Versionen sind immutable-versioniert (DRAFT ->
-- ACTIVE -> SUPERSEDED). Diese Migration ergaenzt zwei Faelle:
--
--  * Edit: eine DRAFT-Version darf vor dem Approve noch am YAML
--    geaendert werden. Fachlich verbindlich sind nur ACTIVE/SUPERSEDED.
--    (keine DB-Spalte noetig - Pruefung im Service.)
--  * Soft-Delete: Admin entfernt eine DRAFT- oder SUPERSEDED-Version
--    aus den Admin-Listen. ACTIVE-Versionen sind bewusst nicht
--    loeschbar (Schutz gegen versehentliche Deaktivierung).

ALTER TABLE context_profile
    ADD COLUMN deleted_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_context_profile_deleted_at
    ON context_profile (deleted_at)
    WHERE deleted_at IS NOT NULL;
