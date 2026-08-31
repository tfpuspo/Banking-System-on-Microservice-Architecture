package com.banking.transaction.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "from_account_id")
    private UUID fromAccountId;

    @Column(name = "to_account_id")
    private UUID toAccountId;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String type = "TRANSFER";

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING | COMPLETED | FAILED

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected TransactionEntity() {}

    public TransactionEntity(UUID fromAccountId, UUID toAccountId, long amountMinorUnits, String currency, String idempotencyKey) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amountMinorUnits = amountMinorUnits;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getId() { return id; }
    public UUID getFromAccountId() { return fromAccountId; }
    public UUID getToAccountId() { return toAccountId; }
    public long getAmountMinorUnits() { return amountMinorUnits; }
    public String getCurrency() { return currency; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; this.updatedAt = Instant.now(); }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String reason) { this.failureReason = reason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
}
