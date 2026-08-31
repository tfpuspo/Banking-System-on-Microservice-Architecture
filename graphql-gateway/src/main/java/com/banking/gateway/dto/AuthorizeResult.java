package com.banking.gateway.dto;

public record AuthorizeResult(boolean allowed, String reason) {}
