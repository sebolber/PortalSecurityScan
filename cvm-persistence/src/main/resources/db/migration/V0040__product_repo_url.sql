-- Iteration 76 (CVM-313): Produkte koennen optional eine Git-
-- Repository-URL hinterlegen, die der Reachability-Agent fuer
-- den JGit-Checkout nutzt. Nullable, damit Produkte ohne Git-
-- Repo (z.B. Closed-Source-Drittsoftware) weiterhin einlesbar
-- bleiben.

ALTER TABLE product
    ADD COLUMN IF NOT EXISTS repo_url VARCHAR(512);

COMMENT ON COLUMN product.repo_url IS
    'Git-Repository-URL (https oder ssh). Optional. Wird von der '
    'Reachability-Funktion fuer den JGit-Checkout verwendet.';
