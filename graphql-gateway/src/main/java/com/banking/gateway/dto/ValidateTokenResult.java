package com.banking.gateway.dto;

import java.util.List;

public record ValidateTokenResult(boolean valid, String userId, List<String> roles) {}
