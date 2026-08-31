-- account-service schema
-- Matches proto/account.proto: Account, CreateAccountRequest, AccountType, AccountStatus

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE accounts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL,               -- references auth-service's users.id (cross-service, no FK)
    account_type            VARCHAR(20) NOT NULL,        -- CHECKING | SAVINGS
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | FROZEN | CLOSED
    currency                CHAR(3) NOT NULL DEFAULT 'USD',
    balance_minor_units     BIGINT NOT NULL DEFAULT 0,   -- cached balance; ledger-service is the source of truth
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_status ON accounts(status);
