package com.banking.account.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String type; // CHECKING | SAVINGS

    @Column(nullable = false)
    private String status = "ACTIVE"; // ACTIVE | FROZEN | CLOSED

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_minor_units", nullable = false)
    private long balanceMinorUnits = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AccountEntity() {
        // required by JPA
    }

    public AccountEntity(UUID userId, String type, String currency) {
        this.userId = userId;
        this.type = type;
        this.currency = currency;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrency() { return currency; }
    public long getBalanceMinorUnits() { return balanceMinorUnits; }
    public Instant getCreatedAt() { return createdAt; }
}
