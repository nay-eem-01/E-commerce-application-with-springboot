-- Module: ecommerce-payment takes ownership of payments (previously written by ecommerce-order)
-- and adds what online providers need. Existing rows are cash-on-delivery attempts.

ALTER TABLE payments RENAME COLUMN provider_reference TO provider_transaction_id;

ALTER TABLE payments
    ADD COLUMN order_number        VARCHAR(16),
    ADD COLUMN reference           VARCHAR(40),
    ADD COLUMN provider            VARCHAR(32) NOT NULL DEFAULT 'cod',
    ADD COLUMN currency            VARCHAR(3)  NOT NULL DEFAULT 'BDT',
    ADD COLUMN provider_payment_id VARCHAR(100),
    ADD COLUMN failure_reason      VARCHAR(200),
    ADD COLUMN expires_at          TIMESTAMPTZ,
    ADD COLUMN refunded_at         TIMESTAMPTZ,
    ADD COLUMN version             BIGINT NOT NULL DEFAULT 0;

-- Drop the old status check before converting VOIDED, which the new set calls CANCELLED.
ALTER TABLE payments DROP CONSTRAINT payments_status_check;

UPDATE payments p SET order_number = o.order_number FROM orders o WHERE o.id = p.order_id;
UPDATE payments SET reference = 'PAY-' || lpad(id::text, 12, '0') WHERE reference IS NULL;
UPDATE payments SET status = 'CANCELLED' WHERE status = 'VOIDED';

ALTER TABLE payments
    ALTER COLUMN order_number SET NOT NULL,
    ALTER COLUMN reference SET NOT NULL,
    ALTER COLUMN provider DROP DEFAULT,
    ADD CONSTRAINT payments_reference_unique UNIQUE (reference),
    ADD CONSTRAINT payments_status_check CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED', 'EXPIRED', 'REFUNDED')),
    ADD CONSTRAINT payments_currency_check CHECK (currency = upper(currency));

-- Callbacks find a payment by the provider's id; the expiry job scans pending online payments.
CREATE UNIQUE INDEX payments_provider_payment_idx ON payments (provider, provider_payment_id) WHERE provider_payment_id IS NOT NULL;
CREATE INDEX payments_pending_expiry_idx ON payments (expires_at) WHERE status = 'PENDING' AND expires_at IS NOT NULL;

-- Append-only audit of every step (summaries only: never raw payloads, credentials or tokens).
CREATE TABLE payment_events (
    id          BIGSERIAL PRIMARY KEY,
    payment_id  BIGINT NOT NULL REFERENCES payments (id) ON DELETE CASCADE,
    kind        VARCHAR(20) NOT NULL,
    detail      VARCHAR(300),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX payment_events_payment_idx ON payment_events (payment_id, id);

CREATE FUNCTION append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER payment_events_no_update
    BEFORE UPDATE ON payment_events
    FOR EACH ROW EXECUTE FUNCTION append_only();
