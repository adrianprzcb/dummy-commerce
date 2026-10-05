CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    source_event_id UUID NOT NULL,
    user_id UUID NOT NULL,
    order_id UUID,
    channel VARCHAR(30) NOT NULL,
    recipient VARCHAR(320) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,

    CONSTRAINT uk_notifications_source_event_id
        UNIQUE (source_event_id),

    CONSTRAINT chk_notifications_channel
        CHECK (
            channel IN (
                'EMAIL'
            )
        ),

    CONSTRAINT chk_notifications_status
        CHECK (
            status IN (
                'PENDING',
                'SENT',
                'FAILED'
            )
        ),

    CONSTRAINT chk_notifications_recipient_not_blank
        CHECK (
            LENGTH(TRIM(recipient)) > 0
        ),

    CONSTRAINT chk_notifications_subject_not_blank
        CHECK (
            LENGTH(TRIM(subject)) > 0
        ),

    CONSTRAINT chk_notifications_message_not_blank
        CHECK (
            LENGTH(TRIM(message)) > 0
        ),

    CONSTRAINT chk_notifications_sent_at
        CHECK (
            (
                status = 'SENT'
                AND sent_at IS NOT NULL
            )
            OR
            (
                status <> 'SENT'
                AND sent_at IS NULL
            )
        )
);

CREATE INDEX idx_notifications_user_id
    ON notifications(user_id);

CREATE INDEX idx_notifications_order_id
    ON notifications(order_id);

CREATE INDEX idx_notifications_status
    ON notifications(status);