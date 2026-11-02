-- Module: ecommerce-notification. Emails are queued here in the same transaction as the order
-- change that caused them, then sent by a dispatcher (FOR UPDATE SKIP LOCKED, retries).

CREATE TABLE notification_outbox (
    id                  BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    dedup_key           VARCHAR(100) NOT NULL UNIQUE,
    recipient           VARCHAR(254) NOT NULL,
    subject             VARCHAR(200) NOT NULL,
    body_text           TEXT NOT NULL,
    body_html           TEXT NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts            INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    next_attempt_at     TIMESTAMPTZ NOT NULL,
    last_error          VARCHAR(300),
    sent_at             TIMESTAMPTZ
);

-- The dispatcher's scan: only pending rows, in due order.
CREATE INDEX notification_outbox_due_idx ON notification_outbox (next_attempt_at, id) WHERE status = 'PENDING';
CREATE INDEX notification_outbox_sent_idx ON notification_outbox (sent_at) WHERE status = 'SENT';

INSERT INTO privileges (name) VALUES ('NOTIFICATION_MANAGE');
INSERT INTO roles_privileges (role_id, privilege_id)
SELECT r.id, p.id FROM roles r, privileges p WHERE r.name = 'ADMIN' AND p.name = 'NOTIFICATION_MANAGE';
