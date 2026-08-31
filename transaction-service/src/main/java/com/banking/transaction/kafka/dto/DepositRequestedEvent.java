package com.banking.transaction.kafka.dto;

public record DepositRequestedEvent(
    String transactionId,
    String toAccountId,
    long amountMinorUnits,
    String currency,
    String idempotencyKey
) {}
