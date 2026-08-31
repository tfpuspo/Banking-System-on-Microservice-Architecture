package com.banking.ledger.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Double-entry: a single transfer produces TWO rows — a negative (debit)
 * entry on the source account, a positive (credit) entry on the
 * destination — always summing to zero across the pair. An account's
 * balance is simply the SUM of its entries; there is no separate "balance"
 * column to accidentally drift out of sync with the journal.
 */
@Entity
@Table(
    name = "journal_entries",
    uniqueConstraints = @UniqueConstraint(columnNames = {"idempotency_key", "account_id"})
)
public class JournalEntryEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits; // positive = credit, negative = debit

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected JournalEntryEntity() {}

    public JournalEntryEntity(UUID transactionId, UUID accountId, long amountMinorUnits, String currency, String idempotencyKey) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.amountMinorUnits = amountMinorUnits;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getId() { return id; }
    public UUID getTransactionId() { return transactionId; }
    public UUID getAccountId() { return accountId; }
    public long getAmountMinorUnits() { return amountMinorUnits; }
    public String getCurrency() { return currency; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
}
