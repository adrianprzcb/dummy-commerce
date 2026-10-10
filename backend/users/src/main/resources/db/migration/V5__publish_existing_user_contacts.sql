WITH contacts AS MATERIALIZED (
    SELECT gen_random_uuid() AS message_id, u.id AS user_id, u.email, u.created_at
    FROM users u
    WHERE NOT EXISTS (
        SELECT 1 FROM outbox_messages o
        WHERE o.aggregate_id = u.id AND o.topic = 'dummy-commerce.users.user-registered.v1'
    )
)
INSERT INTO outbox_messages(id, aggregate_id, topic, message_key, payload, status, created_at)
SELECT message_id, user_id, 'dummy-commerce.users.user-registered.v1', user_id::text,
       json_build_object('messageId', message_id, 'userId', user_id, 'email', email, 'occurredAt', created_at)::text,
       'PENDING', created_at
FROM contacts;
