-- Go-Live-Nachzug zu Iteration 14 (CVM-33): Delta-Summaries werden
-- persistiert, damit Audit/Executive-Report auf sie zugreifen kann.

CREATE TABLE IF NOT EXISTS scan_delta_summary (
    id                 UUID PRIMARY KEY,
    scan_id            UUID NOT NULL REFERENCES scan(id) ON DELETE CASCADE,
    previous_scan_id   UUID REFERENCES scan(id),
    short_text         TEXT NOT NULL,
    long_text          TEXT NOT NULL,
    llm_aufgerufen     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_scan_delta_summary_scan
    ON scan_delta_summary (scan_id, created_at DESC);
