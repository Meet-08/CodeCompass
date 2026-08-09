CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE repository_files
(
    id          UUID                           NOT NULL,
    created_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    codebase_id UUID                           NOT NULL,
    path        VARCHAR(1024)                  NOT NULL,
    language    VARCHAR(255),
    checksum    VARCHAR(255),
    size        BIGINT,
    CONSTRAINT pk_repository_files PRIMARY KEY (id),
    CONSTRAINT uk_repository_files_codebase_path UNIQUE (codebase_id, path),
    CONSTRAINT uk_repository_files_id_codebase UNIQUE (id, codebase_id),
    CONSTRAINT fk_repository_files_on_codebase FOREIGN KEY (codebase_id) REFERENCES codebases (id)
);

CREATE INDEX idx_repository_files_codebase ON repository_files (codebase_id);

CREATE TABLE code_chunks
(
    id          UUID                           NOT NULL,
    created_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    file_id     UUID                           NOT NULL,
    codebase_id UUID                           NOT NULL,
    chunk_index INTEGER                        NOT NULL,
    content     TEXT                           NOT NULL,
    embedding   halfvec(3072),
    language    VARCHAR(255),
    path        VARCHAR(1024)                  NOT NULL,
    start_line  INTEGER,
    end_line    INTEGER,
    commit_sha  VARCHAR(255),
    CONSTRAINT pk_code_chunks PRIMARY KEY (id),
    CONSTRAINT uk_code_chunks_file_chunk_index UNIQUE (file_id, chunk_index),
    CONSTRAINT fk_code_chunks_on_file_and_codebase FOREIGN KEY (file_id, codebase_id)
        REFERENCES repository_files (id, codebase_id)
);

CREATE INDEX idx_code_chunks_codebase ON code_chunks (codebase_id);
CREATE INDEX idx_code_chunks_file ON code_chunks (file_id);
