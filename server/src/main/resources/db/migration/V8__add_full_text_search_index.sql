CREATE INDEX idx_code_chunks_content_fts
    ON code_chunks
    USING GIN (to_tsvector('simple', content));
