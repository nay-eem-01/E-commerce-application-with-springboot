-- Module: ecommerce-order. Orders are snapshots; history is append-only.

CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    order_number    VARCHAR(16) NOT NULL UNIQUE,
    user_id         BIGINT REFERENCES users (id) ON DELETE SET NULL,
    customer_name   VARCHAR(80) NOT NULL,
    phone           VARCHAR(16) NOT NULL,
    email           VARCHAR(254),
    district        VARCHAR(40) NOT NULL,
    area            VARCHAR(80) NOT NULL,
    address_line    VARCHAR(250) NOT NULL,
    delivery_note   VARCHAR(200),
    status          VARCHAR(16) NOT NULL CHECK (status IN ('PLACED', 'PACKED', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'RETURNED')),
    payment_method  VARCHAR(16) NOT NULL CHECK (payment_method IN ('COD', 'BKASH', 'NAGAD', 'CARD')),
    subtotal        NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    delivery_fee    NUMERIC(12, 2) NOT NULL CHECK (delivery_fee >= 0),
    total           NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    deposit_total   NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (deposit_total >= 0),
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    placed_at       TIMESTAMPTZ NOT NULL,
    delivered_at    TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT orders_total_adds_up CHECK (total = subtotal + delivery_fee)
);
CREATE INDEX orders_user_idx ON orders (user_id, placed_at DESC);
CREATE INDEX orders_status_idx ON orders (status, placed_at DESC);
CREATE INDEX orders_phone_idx ON orders (phone);

CREATE TABLE order_items (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id       BIGINT NOT NULL REFERENCES products (id),
    option_id        BIGINT REFERENCES product_options (id) ON DELETE SET NULL,
    product_slug     VARCHAR(120) NOT NULL,
    product_name     VARCHAR(140) NOT NULL,
    option_label     VARCHAR(100),
    unit_price       NUMERIC(12, 2) NOT NULL CHECK (unit_price > 0),
    quantity         INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 10),
    line_total       NUMERIC(12, 2) NOT NULL CHECK (line_total = unit_price * quantity),
    preorder         BOOLEAN NOT NULL DEFAULT FALSE,
    deposit_per_unit NUMERIC(12, 2) NOT NULL DEFAULT 0,
    thumbnail_url    VARCHAR(500)
);
CREATE INDEX order_items_order_idx ON order_items (order_id);
CREATE INDEX order_items_product_idx ON order_items (product_id);

CREATE TABLE order_status_history (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    from_status VARCHAR(16),
    to_status   VARCHAR(16) NOT NULL,
    note        VARCHAR(200),
    changed_by  VARCHAR(40) NOT NULL,
    changed_at  TIMESTAMPTZ NOT NULL
);
CREATE INDEX order_status_history_order_idx ON order_status_history (order_id, id);

-- History is append-only: the database refuses updates, not just the code. (Rows go
-- only when their order is deleted, which the application never does.)
CREATE FUNCTION order_status_history_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'order_status_history is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER order_status_history_no_update
    BEFORE UPDATE ON order_status_history
    FOR EACH ROW EXECUTE FUNCTION order_status_history_append_only();

CREATE TABLE payments (
    id                 BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    order_id           BIGINT NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    method             VARCHAR(16) NOT NULL CHECK (method IN ('COD', 'BKASH', 'NAGAD', 'CARD')),
    status             VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'VOIDED', 'REFUNDED')),
    amount             NUMERIC(12, 2) NOT NULL CHECK (amount >= 0),
    provider_reference VARCHAR(100),
    paid_at            TIMESTAMPTZ
);
CREATE INDEX payments_order_idx ON payments (order_id, id DESC);
