-- Produktkatalog: Produkte und ihre Versionen.
-- Eine ProductVersion gehoert zu genau einem Produkt und referenziert
-- optional einen Git-Commit (Reproduzierbarkeit).

CREATE TABLE product (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    key TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_product_key UNIQUE (key)
);

CREATE TABLE product_version (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    version TEXT NOT NULL,
    git_commit TEXT,
    released_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ,
    CONSTRAINT uq_product_version UNIQUE (product_id, version)
);

CREATE INDEX idx_product_version_product ON product_version (product_id);
