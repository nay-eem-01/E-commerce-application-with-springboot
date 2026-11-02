-- Module: ecommerce-review. One review per buyer per product.

CREATE TABLE reviews (
    id          BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    product_id  BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    body        VARCHAR(1000) NOT NULL,
    author_name VARCHAR(40) NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('PUBLISHED', 'HIDDEN')),
    CONSTRAINT reviews_one_per_buyer UNIQUE (product_id, user_id)
);
CREATE INDEX reviews_product_idx ON reviews (product_id, status, creation_date DESC);
