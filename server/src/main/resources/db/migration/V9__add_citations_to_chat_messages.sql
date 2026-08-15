ALTER TABLE chat_messages
    ADD COLUMN citations JSONB NOT NULL DEFAULT '[]'::jsonb;
