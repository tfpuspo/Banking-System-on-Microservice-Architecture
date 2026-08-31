package com.banking.transaction.kafka.dto;

public record LedgerResultEvent(
    String transactionId,
    boolean success,
    String reason // populated on failure, e.g. "insufficient_funds"
) {}
