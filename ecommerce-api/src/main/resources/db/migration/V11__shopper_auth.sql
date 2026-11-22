ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN google_subject VARCHAR(255) UNIQUE;
ALTER TABLE users DROP CONSTRAINT users_has_sign_in;
ALTER TABLE users ADD CONSTRAINT users_has_sign_in CHECK (
    phone IS NOT NULL OR (email IS NOT NULL AND (password_hash IS NOT NULL OR google_subject IS NOT NULL))
);

CREATE TABLE shopper_email_links (
    email VARCHAR(254) NOT NULL,
    purpose VARCHAR(16) NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (email, purpose)
);
