CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL UNIQUE,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT chk_payments_amount
        CHECK (amount > 0),

    CONSTRAINT chk_payments_currency
        CHECK (
            currency ~ '^[A-Z]{3}$'
        ),

    CONSTRAINT chk_payments_status
        CHECK (
            status IN (
                'PENDING',
                'COMPLETED',
                'FAILED',
                'REFUNDED'
            )
        )
);

CREATE INDEX idx_payments_status
    ON payments(status);