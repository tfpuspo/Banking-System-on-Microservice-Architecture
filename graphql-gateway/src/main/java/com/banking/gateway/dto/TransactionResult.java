package com.banking.gateway.dto;

public record TransactionResult(
    String transactionId,
    String fromAccountId,
    String toAccountId,
    long amountMinorUnits,
    String currency,
    String status,
    String idempotencyKey,
    String createdAt
) {}
