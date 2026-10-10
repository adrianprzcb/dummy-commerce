CREATE TABLE inbox_messages (
    message_id UUID PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
