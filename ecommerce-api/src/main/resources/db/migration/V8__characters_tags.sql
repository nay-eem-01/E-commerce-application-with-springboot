-- Characters are the shop's own collections (Kitsune, Ramen Cat, Ronin); a product belongs to at most one.
CREATE TABLE characters (
    id                  BIGSERIAL PRIMARY KEY,
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

ALTER TABLE products ADD COLUMN character_id BIGINT REFERENCES characters (id);
CREATE INDEX products_character_idx ON products (character_id);

-- Filterable attributes in groups ("Capacity" = "750 ml", "Material" = "Steel"; later "Size" = "M").
CREATE TABLE product_tags (
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    tag_group  VARCHAR(30) NOT NULL,
    tag_value  VARCHAR(40) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT product_tags_unique UNIQUE (product_id, tag_group, tag_value)
);
CREATE INDEX product_tags_value_idx ON product_tags (tag_group, tag_value);

-- A category can be announced before it has products ("Apparel, coming soon").
ALTER TABLE categories ADD COLUMN coming_soon BOOLEAN NOT NULL DEFAULT FALSE;
