CREATE INDEX idx_code_chunks_embedding ON code_chunks
    USING hnsw (embedding halfvec_cosine_ops);