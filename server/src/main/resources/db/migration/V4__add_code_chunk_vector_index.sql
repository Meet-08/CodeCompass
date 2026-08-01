CREATE INDEX idx_code_chunks_embedding_cosine
    ON code_chunks USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;
