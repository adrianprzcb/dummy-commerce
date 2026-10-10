ALTER TABLE outbox_messages
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_error VARCHAR(2000),
    ADD COLUMN last_attempt_at TIMESTAMPTZ;

CREATE INDEX idx_outbox_published_at ON outbox_messages(published_at) WHERE status = 'PUBLISHED';

CREATE INDEX idx_inbox_processed_at ON inbox_messages(processed_at);
