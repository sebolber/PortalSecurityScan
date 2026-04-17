-- Baseline-Migration fuer den CVE-Relevance-Manager (CVM)
-- Aktiviert die fuer das Fachmodell benoetigten PostgreSQL-Extensions und
-- legt die zentrale Audit-Tabelle an. Fachliche Tabellen folgen ab V0001.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

CREATE TABLE audit_trail (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor TEXT NOT NULL,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id UUID,
    payload JSONB
);

CREATE INDEX idx_audit_trail_entity ON audit_trail (entity_type, entity_id);
CREATE INDEX idx_audit_trail_occurred_at ON audit_trail (occurred_at);
