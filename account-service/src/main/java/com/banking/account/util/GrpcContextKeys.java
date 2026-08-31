package com.banking.account.util;

import io.grpc.Context;

/**
 * gRPC's Context is the correct way to pass per-request data (like an auth
 * token) through a call without threading it through every method signature.
 * Unlike a plain ThreadLocal, Context is designed by gRPC specifically to
 * survive thread hops within a single call's lifecycle.
 */
public final class GrpcContextKeys {

    public static final Context.Key<String> AUTH_TOKEN = Context.key("auth-token");

    private GrpcContextKeys() {}
}
