-- Iteration 20 -- Waiver-Management (CVM-51).
--
-- Ein Waiver verweist auf das APPROVED-Assessment, das ihn
-- rechtfertigt. Er hat ein validUntil; 30 Tage vorher wechselt der
-- Status auf EXPIRING_SOON, beim Ablauf auf EXPIRED. Ein
-- revoke()-Aufruf setzt REVOKED.

CREATE TABLE IF NOT EXISTS waiver (
    id                  UUID PRIMARY KEY,
    assessment_id       UUID NOT NULL REFERENCES assessment(id),
    reason              TEXT NOT NULL,
    granted_by          TEXT NOT NULL,
    valid_until         TIMESTAMPTZ NOT NULL,
    review_interval_days INTEGER NOT NULL DEFAULT 90,
    status              TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ,
    revoked_by          TEXT,
    revoked_at          TIMESTAMPTZ,
    extended_by         TEXT,
    extended_at         TIMESTAMPTZ,
    CONSTRAINT ck_waiver_status
        CHECK (status IN ('ACTIVE','EXPIRING_SOON','EXPIRED','REVOKED'))
);

CREATE INDEX IF NOT EXISTS idx_waiver_status_valid
    ON waiver (status, valid_until);

CREATE INDEX IF NOT EXISTS idx_waiver_assessment
    ON waiver (assessment_id);
