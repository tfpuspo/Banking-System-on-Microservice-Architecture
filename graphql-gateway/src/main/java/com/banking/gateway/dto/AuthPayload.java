package com.banking.gateway.dto;

public record AuthPayload(String accessToken, String refreshToken, long expiresIn) {}
