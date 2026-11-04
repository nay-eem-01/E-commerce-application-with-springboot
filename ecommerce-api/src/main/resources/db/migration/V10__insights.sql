-- Demand counters for the admin insights dashboard. Aggregates only: no user, IP or browser is stored.
CREATE TABLE product_view_daily (
    day        DATE    NOT NULL,
    product_id BIGINT  NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    views      INTEGER NOT NULL CHECK (views >= 0),
    PRIMARY KEY (day, product_id)
);
CREATE INDEX product_view_daily_product_idx ON product_view_daily (product_id);

CREATE TABLE search_term_daily (
    day          DATE        NOT NULL,
    term         VARCHAR(60) NOT NULL,
    searches     INTEGER     NOT NULL CHECK (searches >= 0),
    zero_results INTEGER     NOT NULL CHECK (zero_results >= 0 AND zero_results <= searches),
    PRIMARY KEY (day, term)
);

INSERT INTO privileges (name) VALUES ('INSIGHTS_READ');
INSERT INTO roles_privileges (role_id, privilege_id)
SELECT r.id, p.id FROM roles r, privileges p WHERE r.name = 'ADMIN' AND p.name = 'INSIGHTS_READ';
