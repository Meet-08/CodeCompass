ALTER TABLE code_chunks
    DROP CONSTRAINT fk_code_chunks_on_file_and_codebase;

CREATE TABLE chat_messages
(
    id          UUID                           NOT NULL,
    created_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    session_id  UUID                           NOT NULL,
    role        VARCHAR(255)                   NOT NULL,
    content     TEXT                           NOT NULL,
    token_count INTEGER,
    CONSTRAINT pk_chat_messages PRIMARY KEY (id)
);

CREATE TABLE chat_sessions
(
    id          UUID                           NOT NULL,
    created_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    user_id     UUID                           NOT NULL,
    codebase_id UUID                           NOT NULL,
    title       VARCHAR(255)                   NOT NULL,
    CONSTRAINT pk_chat_sessions PRIMARY KEY (id)
);

CREATE INDEX idx_chat_message_session_created ON chat_messages (session_id, created_at);

ALTER TABLE chat_messages
    ADD CONSTRAINT FK_CHAT_MESSAGES_ON_SESSION FOREIGN KEY (session_id) REFERENCES chat_sessions (id);

ALTER TABLE chat_sessions
    ADD CONSTRAINT FK_CHAT_SESSIONS_ON_CODEBASE FOREIGN KEY (codebase_id) REFERENCES codebases (id);

CREATE INDEX idx_chat_session_codebase ON chat_sessions (codebase_id);

ALTER TABLE chat_sessions
    ADD CONSTRAINT FK_CHAT_SESSIONS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

CREATE INDEX idx_chat_session_user ON chat_sessions (user_id);

ALTER TABLE code_chunks
    ADD CONSTRAINT FK_CODE_CHUNKS_ON_CODEBASE FOREIGN KEY (codebase_id) REFERENCES codebases (id);

ALTER TABLE code_chunks
    ADD CONSTRAINT FK_CODE_CHUNKS_ON_FILE FOREIGN KEY (file_id) REFERENCES repository_files (id);