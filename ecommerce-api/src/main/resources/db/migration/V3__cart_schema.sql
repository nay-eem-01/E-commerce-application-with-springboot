-- Module: ecommerce-cart. Ids only; prices are read live from the catalog.
-- NULLS NOT DISTINCT (PostgreSQL 15+) makes (user, product, no option) unique too.

CREATE TABLE cart_items (
    id          BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    option_id   BIGINT REFERENCES product_options (id) ON DELETE CASCADE,
    quantity    INTEGER NOT NULL CHECK (quantity BETWEEN 1 AND 10),
    CONSTRAINT cart_items_line_unique UNIQUE NULLS NOT DISTINCT (user_id, product_id, option_id)
);
CREATE INDEX cart_items_user_idx ON cart_items (user_id);

CREATE TABLE wishlist_items (
    id          BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id  BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT wishlist_items_unique UNIQUE (user_id, product_id)
);
