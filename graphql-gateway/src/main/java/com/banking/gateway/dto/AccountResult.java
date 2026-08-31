package com.banking.gateway.dto;

public record AccountResult(
    String accountId,
    String userId,
    String type,
    String status,
    String currency,
    long balanceMinorUnits,
    String createdAt
) {}
