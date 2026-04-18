-- Iteration 16 -- Fix-Verifikation (CVM-41).
--
-- Erweitert mitigation_plan um das Ergebnis der LLM-gestuetzten
-- Fix-Verifikation (Grade A/B/C + Evidenz-Typ + Zeitstempel).
-- Evidenz-Commits selbst werden nicht als Tabelle gehalten; sie
-- leben als ai_source_ref-Eintraege am zugehoerigen ai_suggestion.

ALTER TABLE mitigation_plan
    ADD COLUMN IF NOT EXISTS verification_grade TEXT,
    ADD COLUMN IF NOT EXISTS verification_evidence_type TEXT,
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ;

ALTER TABLE mitigation_plan
    DROP CONSTRAINT IF EXISTS ck_mitigation_verification_grade;
ALTER TABLE mitigation_plan
    ADD CONSTRAINT ck_mitigation_verification_grade
    CHECK (verification_grade IS NULL OR verification_grade IN (
        'A','B','C','UNKNOWN'));

ALTER TABLE mitigation_plan
    DROP CONSTRAINT IF EXISTS ck_mitigation_verification_evidence;
ALTER TABLE mitigation_plan
    ADD CONSTRAINT ck_mitigation_verification_evidence
    CHECK (verification_evidence_type IS NULL OR verification_evidence_type IN (
        'EXPLICIT_CVE_MENTION','FIX_COMMIT_MATCH','INFERRED','NONE'));

-- ai_source_ref.kind um GIT_COMMIT erweitern, damit Commits sauber
-- typisiert referenziert werden koennen (bisher nur CODE_REF).
ALTER TABLE ai_source_ref
    DROP CONSTRAINT IF EXISTS ck_ai_source_ref_kind;
ALTER TABLE ai_source_ref
    ADD CONSTRAINT ck_ai_source_ref_kind
    CHECK (kind IN (
        'PROFILE_PATH','CVE','RULE','DOCUMENT','CODE_REF','GIT_COMMIT'));
