package com.banking.ledger.kafka.dto;

public record TransferRequestedEvent(
    String transactionId,
    String fromAccountId,
    String toAccountId,
    long amountMinorUnits,
    String currency,
    String idempotencyKey
) {}
