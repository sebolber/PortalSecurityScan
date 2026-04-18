-- Iteration 12 -- RAG-Pipeline (CVM-31).
--
-- Eine Tabelle ai_embedding mit pgvector-Spalte. Embedding-Dimension
-- ist auf 1536 fix gesetzt (Default Anthropic Embeddings 1536,
-- Ollama nomic-embed-text 768 wird mit Padding/Truncation auf 1536
-- normalisiert - das uebernimmt der Adapter, nicht die DB).
--
-- Index ueber pgvector ivfflat / cosine_ops. Trainings-Cluster bleibt
-- mit lists=100 konservativ; ein Re-Tuning macht Iteration 21.

CREATE TABLE IF NOT EXISTS ai_embedding (
    id              UUID PRIMARY KEY,
    document_type   TEXT NOT NULL,
    document_ref    TEXT NOT NULL,
    chunk_index     INTEGER NOT NULL DEFAULT 0,
    chunk_text      TEXT NOT NULL,
    embedding       VECTOR(1536) NOT NULL,
    model_id        TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_ai_embedding_type
        CHECK (document_type IN (
            'ASSESSMENT','ADVISORY','COMMIT','PROFILE_SNAPSHOT','DOC')),
    CONSTRAINT uq_ai_embedding_ref_chunk
        UNIQUE (document_type, document_ref, chunk_index, model_id)
);

CREATE INDEX IF NOT EXISTS idx_ai_embedding_doc
    ON ai_embedding (document_type, document_ref);

CREATE INDEX IF NOT EXISTS idx_ai_embedding_model
    ON ai_embedding (model_id);

CREATE INDEX IF NOT EXISTS idx_ai_embedding_vector
    ON ai_embedding USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
