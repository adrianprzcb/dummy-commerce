ALTER TABLE order_compensations
    ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'RELEASE_PENDING',
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_error VARCHAR(2000),
    ADD COLUMN next_attempt_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_compensation_status CHECK (status IN ('RELEASE_PENDING', 'REFUND_PENDING', 'REFUND_FAILED', 'COMPLETED'));

UPDATE order_compensations SET status = 'COMPLETED'
WHERE order_id IN (SELECT id FROM orders WHERE status = 'CANCELLED');

CREATE INDEX idx_compensation_retry ON order_compensations(next_attempt_at) WHERE status = 'REFUND_FAILED';
