package com.banking.ledger.kafka.dto;

public record LedgerResultEvent(
    String transactionId,
    boolean success,
    String reason
) {}
