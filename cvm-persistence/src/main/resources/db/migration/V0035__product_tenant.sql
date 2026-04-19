-- Iteration 62A (CVM-62): Produkt + Produkt-Version bekommen eine
-- verpflichtende Mandanten-Zuordnung. Bisher waren beide Tabellen
-- mandantenuebergreifend - jeder angemeldete Admin sah alle Produkte.
--
-- Backfill: alle bestehenden Eintraege werden auf den Default-Mandanten
-- (is_default = TRUE) gesetzt. Anschliessend wird die Spalte NOT NULL.
-- Neue Produkte werden ueber den Service gegen den TenantContext
-- angelegt.

ALTER TABLE product ADD COLUMN tenant_id UUID;

UPDATE product
   SET tenant_id = (SELECT id FROM tenant WHERE is_default = TRUE LIMIT 1)
 WHERE tenant_id IS NULL;

ALTER TABLE product ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE product
    ADD CONSTRAINT fk_product_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant (id);
CREATE INDEX idx_product_tenant ON product (tenant_id);

-- Key-Uniqueness auf (tenant_id, key) umstellen: verschiedene Mandanten
-- sollen denselben Produkt-Key nutzen duerfen.
ALTER TABLE product DROP CONSTRAINT IF EXISTS uq_product_key;
ALTER TABLE product
    ADD CONSTRAINT uq_product_tenant_key UNIQUE (tenant_id, key);

-- Produkt-Version uebernimmt die Mandantenzuordnung vom Produkt.
ALTER TABLE product_version ADD COLUMN tenant_id UUID;

UPDATE product_version pv
   SET tenant_id = p.tenant_id
  FROM product p
 WHERE pv.product_id = p.id;

ALTER TABLE product_version ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE product_version
    ADD CONSTRAINT fk_product_version_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenant (id);
CREATE INDEX idx_product_version_tenant ON product_version (tenant_id);
