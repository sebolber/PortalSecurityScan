-- Iteration 48 (CVM-98): Umgebung-Soft-Delete.
-- Wir fuegen eine nullable-Spalte {@code deleted_at} hinzu. Ein
-- Wert != NULL markiert die Umgebung als (logisch) entfernt.
-- Bestehende Scans, Findings und Assessments, die auf diese
-- Umgebung verweisen, bleiben erhalten, die Umgebung verschwindet
-- nur aus den Admin- und Auswahl-Listen.

ALTER TABLE environment
    ADD COLUMN deleted_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_environment_deleted_at
    ON environment (deleted_at)
    WHERE deleted_at IS NOT NULL;
