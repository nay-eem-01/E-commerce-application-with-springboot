-- A set is a product of its own (own price and stock) made of other products, e.g. backpack + bottle.
CREATE TABLE product_bundle_items (
    id           BIGSERIAL PRIMARY KEY,
    bundle_id    BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    component_id BIGINT NOT NULL REFERENCES products (id),
    quantity     INTEGER NOT NULL DEFAULT 1 CHECK (quantity BETWEEN 1 AND 10),
    sort_order   INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT product_bundle_items_unique UNIQUE (bundle_id, component_id),
    CONSTRAINT product_bundle_items_not_self CHECK (bundle_id <> component_id)
);
CREATE INDEX product_bundle_items_component_idx ON product_bundle_items (component_id);

-- Early access to a coming-soon category: a phone and/or an email, once per category.
CREATE TABLE early_access_signups (
    id            BIGSERIAL PRIMARY KEY,
    category_id   BIGINT NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    phone         VARCHAR(20),
    email         VARCHAR(254),
    creation_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT early_access_contact CHECK (phone IS NOT NULL OR email IS NOT NULL)
);
CREATE UNIQUE INDEX early_access_phone_unique ON early_access_signups (category_id, phone) WHERE phone IS NOT NULL;
CREATE UNIQUE INDEX early_access_email_unique ON early_access_signups (category_id, lower(email)) WHERE email IS NOT NULL;
