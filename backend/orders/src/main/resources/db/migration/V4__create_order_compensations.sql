CREATE TABLE order_compensations (
    order_id UUID PRIMARY KEY REFERENCES orders(id) ON DELETE CASCADE
);
