-- ledger-service schema
-- Matches proto/ledger.proto: PostEntryRequest, JournalEntry
-- This is the ONLY service allowed to write balance-affecting entries.
-- Double-entry style: every posting has a debit leg and a credit leg.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE journal_entries (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id          UUID NOT NULL,                 -- references transaction-service's transactions.id
    account_id              UUID NOT NULL,                 -- references account-service's accounts.id
    amount_minor_units      BIGINT NOT NULL,                -- positive = credit, negative = debit
    currency                CHAR(3) NOT NULL DEFAULT 'USD',
    idempotency_key         VARCHAR(255) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (idempotency_key, account_id)  -- one posting per account per idempotency key
);

CREATE INDEX idx_journal_account_id ON journal_entries(account_id);
CREATE INDEX idx_journal_transaction_id ON journal_entries(transaction_id);

-- Convenience view: running balance per account, derived from the journal
CREATE VIEW account_balances AS
SELECT account_id, SUM(amount_minor_units) AS balance_minor_units
FROM journal_entries
GROUP BY account_id;
