-- Iteration 49 (CVM-99): Produkt-Version-Soft-Delete.
-- Eine Produkt-Version kann logisch entfernt werden, ohne dass die
-- darauf referenzierenden Scans, Findings und Assessments veraendert
-- werden. Der Eintrag verschwindet dann nur aus den Admin- und
-- Auswahl-Listen.

ALTER TABLE product_version
    ADD COLUMN deleted_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_product_version_deleted_at
    ON product_version (deleted_at)
    WHERE deleted_at IS NOT NULL;
