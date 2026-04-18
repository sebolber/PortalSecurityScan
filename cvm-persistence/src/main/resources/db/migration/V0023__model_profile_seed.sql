-- Iteration 22 -- Modell-Profil-Seed + Default-Tenant (CVM-53).
--
-- Legt die zwei Standard-Modellprofile an, auf die Umgebungen bis
-- zum Rollout eines eigenen Profils zeigen koennen, sowie einen
-- Default-Tenant.  Idempotent ueber ON CONFLICT, damit re-Migration
-- kein Problem ist (Flyway prueft Hash, Seed-Aenderungen bitte als
-- eigenen Versionschritt anlegen).

-- ---------------------------------------------------------------
-- Default-Tenant
-- ---------------------------------------------------------------
INSERT INTO tenant (id, tenant_key, name, active, is_default, created_at)
VALUES ('00000000-0000-0000-0000-000000000001',
        'default',
        'Default-Mandant (Pre-Rollout)',
        TRUE,
        TRUE,
        now())
ON CONFLICT (tenant_key) DO NOTHING;

-- ---------------------------------------------------------------
-- Modellprofil-Seed: Claude Cloud (Produktiv) + Ollama (Fallback).
-- ---------------------------------------------------------------
INSERT INTO llm_model_profile (
        id, profile_key, provider, model_id, model_version,
        cost_budget_eur_monthly, approved_for_gkv_data, created_at)
VALUES ('00000000-0000-0000-0000-0000000000a1',
        'CLAUDE_CLOUD_DEFAULT',
        'CLAUDE_CLOUD',
        'claude-sonnet-4-6',
        'sonnet-4.6',
        100.00,
        TRUE,
        now())
ON CONFLICT (profile_key) DO NOTHING;

INSERT INTO llm_model_profile (
        id, profile_key, provider, model_id, model_version,
        cost_budget_eur_monthly, approved_for_gkv_data, created_at)
VALUES ('00000000-0000-0000-0000-0000000000a2',
        'OLLAMA_ONPREM_FALLBACK',
        'OLLAMA_ONPREM',
        'llama3:8b',
        NULL,
        0.00,
        TRUE,
        now())
ON CONFLICT (profile_key) DO NOTHING;
