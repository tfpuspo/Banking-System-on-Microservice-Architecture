package com.banking.transaction.util;

import io.grpc.Context;

public final class GrpcContextKeys {
    public static final Context.Key<String> AUTH_TOKEN = Context.key("auth-token");
    private GrpcContextKeys() {}
}
