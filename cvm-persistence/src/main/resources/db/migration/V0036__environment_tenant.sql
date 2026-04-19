-- Iteration 62B (CVM-62): Umgebung bekommt eine verpflichtende
-- Mandanten-Zuordnung. Die bestehende TEXT-Spalte `tenant` war eine
-- Freitext-Bezeichnung und wird nicht entfernt (Backward-Compat), aber
-- die echte Isolation kommt ueber `tenant_id` (FK auf tenant.id).
--
-- Backfill: alle bestehenden Eintraege auf den Default-Mandanten.
-- Anschliessend Spalte NOT NULL.

ALTER TABLE environment ADD COLUMN tenant_id UUID;

UPDATE environment
   SET tenant_id = (SELECT id FROM tenant WHERE is_default = TRUE LIMIT 1)
 WHERE tenant_id IS NULL;

ALTER TABLE environment ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE environment
    ADD CONSTRAINT fk_environment_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant (id);
CREATE INDEX idx_environment_tenant ON environment (tenant_id);

-- Key-Uniqueness auf (tenant_id, key) umstellen, damit verschiedene
-- Mandanten denselben Umgebungs-Key nutzen duerfen (z.B. "PROD").
ALTER TABLE environment DROP CONSTRAINT IF EXISTS environment_key_key;
ALTER TABLE environment DROP CONSTRAINT IF EXISTS uq_environment_key;
ALTER TABLE environment
    ADD CONSTRAINT uq_environment_tenant_key UNIQUE (tenant_id, key);
