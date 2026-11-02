-- Module: ecommerce-catalog. Money is NUMERIC(12,2) BDT.

CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    slug        VARCHAR(60) NOT NULL UNIQUE,
    name        VARCHAR(60) NOT NULL,
    label       VARCHAR(8),
    blurb       VARCHAR(160),
    sort_order  INTEGER NOT NULL DEFAULT 0,
    active      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE products (
    id               BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    slug             VARCHAR(120) NOT NULL UNIQUE,
    category_id      BIGINT NOT NULL REFERENCES categories (id),
    name             VARCHAR(140) NOT NULL,
    maker            VARCHAR(80),
    summary          VARCHAR(300) NOT NULL,
    description      TEXT,
    story            TEXT,
    story_quote      VARCHAR(200),
    spec_line        VARCHAR(120),
    price            NUMERIC(12, 2) NOT NULL CHECK (price > 0),
    compare_at_price NUMERIC(12, 2) CHECK (compare_at_price > 0),
    stock            INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    preorder         BOOLEAN NOT NULL DEFAULT FALSE,
    preorder_release DATE,
    deposit          NUMERIC(12, 2) CHECK (deposit >= 0),
    sized            BOOLEAN NOT NULL DEFAULT FALSE,
    option_name      VARCHAR(30),
    status           VARCHAR(16) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    version          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT products_preorder_fields CHECK (NOT preorder OR (preorder_release IS NOT NULL AND deposit IS NOT NULL))
);
CREATE INDEX products_category_idx ON products (category_id, status);
CREATE INDEX products_status_created_idx ON products (status, creation_date DESC);
CREATE INDEX products_status_price_idx ON products (status, price);

CREATE TABLE product_options (
    id           BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    product_id   BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    option_value VARCHAR(60) NOT NULL,
    price_delta  NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (price_delta >= 0),
    stock        INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    swatch       VARCHAR(32),
    sort_order   INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX product_options_product_idx ON product_options (product_id);

CREATE TABLE product_facts (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    label      VARCHAR(40) NOT NULL,
    fact_value VARCHAR(120) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX product_facts_product_idx ON product_facts (product_id);

CREATE TABLE product_box_items (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    item       VARCHAR(120) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX product_box_items_product_idx ON product_box_items (product_id);

CREATE TABLE product_media (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    kind       VARCHAR(16) NOT NULL CHECK (kind IN ('PHOTO', 'SPIN_FRAME', 'STORY')),
    url        VARCHAR(500) NOT NULL,
    alt_text   VARCHAR(160),
    sort_order INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX product_media_product_idx ON product_media (product_id, kind, sort_order);
