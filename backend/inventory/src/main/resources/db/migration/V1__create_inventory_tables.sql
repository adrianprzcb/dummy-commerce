CREATE TABLE inventory_items (
    product_id UUID PRIMARY KEY,
    available_quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL,

    CONSTRAINT chk_inventory_available_quantity
        CHECK (available_quantity >= 0),

    CONSTRAINT chk_inventory_reserved_quantity
        CHECK (reserved_quantity >= 0)
);

CREATE TABLE stock_reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_stock_reservations_inventory
        FOREIGN KEY (product_id)
        REFERENCES inventory_items(product_id),

    CONSTRAINT chk_stock_reservation_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_stock_reservation_status
        CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED')),

    CONSTRAINT uq_stock_reservation_order_product
        UNIQUE (order_id, product_id)
);

CREATE INDEX idx_stock_reservations_order_id
    ON stock_reservations(order_id);