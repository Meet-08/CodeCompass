CREATE TABLE codebases
(
    id              UUID         NOT NULL,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    user_id         UUID         NOT NULL,
    name            VARCHAR(255) NOT NULL,
    clone_url       VARCHAR(255),
    branch          VARCHAR(255),
    status          VARCHAR(255),
    last_commit_sha VARCHAR(255),
    indexed_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT pk_codebases PRIMARY KEY (id)
);

ALTER TABLE codebases
    ADD CONSTRAINT FK_CODEBASES_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE users
DROP
COLUMN provider;

ALTER TABLE users
DROP
COLUMN role;

ALTER TABLE users
    ADD provider VARCHAR(255);

ALTER TABLE users
    ADD role VARCHAR(255);
