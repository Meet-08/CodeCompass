ALTER TABLE code_chunks
    ADD COLUMN symbol_name VARCHAR(512),
    ADD COLUMN symbol_qualified_name VARCHAR(1024),
    ADD COLUMN chunk_type VARCHAR(64),
    ADD COLUMN parent_symbol VARCHAR(512);

CREATE INDEX idx_code_chunks_symbol ON code_chunks (symbol_qualified_name);
