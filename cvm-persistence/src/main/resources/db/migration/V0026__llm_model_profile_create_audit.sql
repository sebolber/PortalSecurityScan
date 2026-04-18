-- Iteration 27 -- Anlage neuer Modellprofile mit Audit (CVM-58, CVM-59).
--
-- Erweitert das bestehende model_profile_change_log um eine Action-Spalte
-- und erlaubt environment_id=NULL fuer PROFILE_CREATED-Eintraege, da
-- die Anlage eines Profils kein Environment-Scope ist.

ALTER TABLE model_profile_change_log
    ADD COLUMN IF NOT EXISTS action TEXT NOT NULL DEFAULT 'PROFILE_SWITCHED';

ALTER TABLE model_profile_change_log
    ALTER COLUMN environment_id DROP NOT NULL;

ALTER TABLE model_profile_change_log
    DROP CONSTRAINT IF EXISTS ck_model_profile_change_log_action;

ALTER TABLE model_profile_change_log
    ADD CONSTRAINT ck_model_profile_change_log_action
    CHECK (action IN ('PROFILE_SWITCHED','PROFILE_CREATED'));

-- PROFILE_SWITCHED-Eintraege muessen weiterhin ein Environment tragen.
ALTER TABLE model_profile_change_log
    DROP CONSTRAINT IF EXISTS ck_model_profile_change_log_env_scope;

ALTER TABLE model_profile_change_log
    ADD CONSTRAINT ck_model_profile_change_log_env_scope
    CHECK (
        (action = 'PROFILE_SWITCHED' AND environment_id IS NOT NULL)
        OR (action = 'PROFILE_CREATED')
    );
