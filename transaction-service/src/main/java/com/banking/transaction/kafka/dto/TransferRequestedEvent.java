package com.banking.transaction.kafka.dto;

/**
 * Published to ledger.commands. Note: no library-level (de)serialization
 * magic here — deliberately using plain Jackson-friendly fields and a
 * hand-written toJson/fromJson pair in each service, since transaction-
 * service and ledger-service don't share code (consistent with how proto
 * files are duplicated per service throughout this project).
 */
public record TransferRequestedEvent(
    String transactionId,
    String fromAccountId,
    String toAccountId,
    long amountMinorUnits,
    String currency,
    String idempotencyKey
) {}
