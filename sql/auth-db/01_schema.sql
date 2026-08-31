-- auth-service schema
-- Matches proto/auth.proto: RegisterRequest, LoginResponse, ValidateTokenResponse, AuthorizeRequest

CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- for gen_random_uuid()

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,       -- bcrypt/argon2 hash, never plaintext
    full_name       VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'active', -- active | suspended | closed
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id      SERIAL PRIMARY KEY,
    name    VARCHAR(50) NOT NULL UNIQUE   -- e.g. 'customer', 'teller', 'admin'
);

-- Many-to-many: a user can have multiple roles
CREATE TABLE user_roles (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id     INT  NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Refresh tokens / sessions, so Logout and revocation are possible
-- (access tokens themselves are stateless JWTs, verified without a DB hit)
CREATE TABLE sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token   VARCHAR(512) NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ NOT NULL,
    revoked         BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_users_email ON users(email);

-- Seed default roles
INSERT INTO roles (name) VALUES ('customer'), ('teller'), ('admin');
