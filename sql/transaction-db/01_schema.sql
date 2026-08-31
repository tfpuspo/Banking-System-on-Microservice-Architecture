-- transaction-service schema
-- Matches proto/transaction.proto: Transaction, TransferRequest, DepositRequest, WithdrawalRequest

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE transactions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_account_id         UUID,                         -- null for deposits
    to_account_id           UUID,                          -- null for withdrawals
    amount_minor_units      BIGINT NOT NULL CHECK (amount_minor_units > 0),
    currency                CHAR(3) NOT NULL DEFAULT 'USD',
    type                    VARCHAR(20) NOT NULL,          -- TRANSFER | DEPOSIT | WITHDRAWAL
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING | COMPLETED | FAILED | REVERSED
    idempotency_key         VARCHAR(255) NOT NULL UNIQUE,  -- prevents duplicate transfers on retry
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_from_account ON transactions(from_account_id);
CREATE INDEX idx_transactions_to_account ON transactions(to_account_id);
CREATE INDEX idx_transactions_status ON transactions(status);
