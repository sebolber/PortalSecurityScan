-- Iteration 38 (CVM-82): Produkt-Soft-Delete.
-- Wir fuegen eine nullable-Spalte {@code deleted_at} hinzu. Ein
-- Wert != NULL markiert das Produkt als (logisch) entfernt. SBOM-
-- Scans fuer bestehende Produkt-Versionen bleiben weiter referenzierbar,
-- tauchen aber in Admin- und Queue-UIs nicht mehr auf.

ALTER TABLE product
    ADD COLUMN deleted_at TIMESTAMPTZ NULL;

CREATE INDEX IF NOT EXISTS idx_product_deleted_at
    ON product (deleted_at)
    WHERE deleted_at IS NOT NULL;
