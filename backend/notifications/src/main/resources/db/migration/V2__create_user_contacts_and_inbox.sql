CREATE TABLE user_contacts (
    user_id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE inbox_messages (
    message_id UUID PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);
