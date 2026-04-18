-- Iteration 21 -- Mandantenfaehigkeit + Modell-Profil (CVM-52).
--
-- Legt die Basistabellen fuer den Produktiv-Rollout an:
--   tenant              ein Eintrag pro Mandant (BKK).
--   llm_model_profile   Modellprofile (Claude Cloud / Ollama on-prem),
--                       inkl. Monatsbudget und GKV-Freigabe.
--   environment.llm_model_profile_id  zeigt auf das aktive Profil.
--   model_profile_change_log  auditierbarer Wechsel-Log (Vier-Augen).
--
-- RLS-Policies auf Sachtabellen werden bewusst erst in einer spaeteren
-- Rollout-Migration aktiviert, siehe offene-punkte.md.

CREATE TABLE IF NOT EXISTS tenant (
    id          UUID PRIMARY KEY,
    tenant_key  TEXT NOT NULL UNIQUE,
    name        TEXT NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Genau ein Default-Mandant.
CREATE UNIQUE INDEX IF NOT EXISTS ux_tenant_default
    ON tenant (is_default) WHERE is_default IS TRUE;

CREATE TABLE IF NOT EXISTS llm_model_profile (
    id                         UUID PRIMARY KEY,
    profile_key                TEXT NOT NULL UNIQUE,
    provider                   TEXT NOT NULL,
    model_id                   TEXT NOT NULL,
    model_version              TEXT,
    cost_budget_eur_monthly    NUMERIC(12,2) NOT NULL DEFAULT 0,
    approved_for_gkv_data      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                 TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                 TIMESTAMPTZ,
    CONSTRAINT ck_llm_model_profile_provider
        CHECK (provider IN ('CLAUDE_CLOUD','OLLAMA_ONPREM'))
);

ALTER TABLE environment
    ADD COLUMN IF NOT EXISTS llm_model_profile_id UUID
    REFERENCES llm_model_profile(id);

CREATE TABLE IF NOT EXISTS model_profile_change_log (
    id                    UUID PRIMARY KEY,
    environment_id        UUID NOT NULL REFERENCES environment(id),
    previous_profile_id   UUID REFERENCES llm_model_profile(id),
    new_profile_id        UUID NOT NULL REFERENCES llm_model_profile(id),
    changed_by            TEXT NOT NULL,
    four_eyes_confirmer   TEXT NOT NULL,
    reason                TEXT,
    changed_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_model_profile_change_env
    ON model_profile_change_log (environment_id, changed_at DESC);
