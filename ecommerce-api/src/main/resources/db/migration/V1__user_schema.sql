-- Module: ecommerce-user. Identity, roles, sessions, sign-in codes, address book.
-- All times are UTC (timestamptz). Ids are BIGSERIAL; public ids live in separate columns.

CREATE TABLE privileges (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE CHECK (name IN ('CUSTOMER', 'ADMIN'))
);

CREATE TABLE roles_privileges (
    role_id      BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    privilege_id BIGINT NOT NULL REFERENCES privileges (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, privilege_id)
);

CREATE TABLE users (
    id                   BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    name                 VARCHAR(80),
    phone                VARCHAR(16) UNIQUE,
    email                VARCHAR(254) UNIQUE,
    password_hash        VARCHAR(100),
    status               VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BANNED')),
    phone_verified_at    TIMESTAMPTZ,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    -- every account can sign in somehow: by phone code or by email + password
    CONSTRAINT users_has_sign_in CHECK (phone IS NOT NULL OR (email IS NOT NULL AND password_hash IS NOT NULL)),
    CONSTRAINT users_email_lowercase CHECK (email = lower(email))
);

CREATE TABLE users_roles (
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE user_sessions (
    id          BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    public_id   UUID NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    user_agent  VARCHAR(200)
);
CREATE INDEX user_sessions_user_idx ON user_sessions (user_id) WHERE active;

CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    session_id  BIGINT NOT NULL REFERENCES user_sessions (id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ
);
CREATE INDEX refresh_tokens_session_idx ON refresh_tokens (session_id);
CREATE INDEX refresh_tokens_user_idx ON refresh_tokens (user_id);

CREATE TABLE otp_verifications (
    id          BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    phone       VARCHAR(16) NOT NULL,
    purpose     VARCHAR(16) NOT NULL CHECK (purpose IN ('SIGN_IN')),
    code_hash   VARCHAR(100) NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    attempts    INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    consumed_at TIMESTAMPTZ
);
CREATE INDEX otp_open_idx ON otp_verifications (phone, purpose, id DESC) WHERE consumed_at IS NULL;

CREATE TABLE addresses (
    id           BIGSERIAL PRIMARY KEY,
    created_by          VARCHAR(40),
    last_modified_by    VARCHAR(40),
    creation_date       TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_modified_date  TIMESTAMPTZ NOT NULL DEFAULT now(),
    user_id      BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    full_name    VARCHAR(80) NOT NULL,
    phone        VARCHAR(16) NOT NULL,
    district     VARCHAR(40) NOT NULL,
    area         VARCHAR(80) NOT NULL,
    address_line VARCHAR(250) NOT NULL,
    is_default   BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX addresses_user_idx ON addresses (user_id);
CREATE UNIQUE INDEX addresses_one_default ON addresses (user_id) WHERE is_default;

-- Seed: privileges (must match com.ecommerce.common.security.Privileges) and roles.
INSERT INTO privileges (name) VALUES
    ('CATEGORY_WRITE'), ('PRODUCT_WRITE'), ('PRODUCT_READ_ALL'), ('ORDER_READ_ALL'),
    ('ORDER_UPDATE'), ('USER_READ'), ('USER_UPDATE'), ('REVIEW_MODERATE');

INSERT INTO roles (name) VALUES ('CUSTOMER'), ('ADMIN');

INSERT INTO roles_privileges (role_id, privilege_id)
SELECT r.id, p.id FROM roles r CROSS JOIN privileges p WHERE r.name = 'ADMIN';
