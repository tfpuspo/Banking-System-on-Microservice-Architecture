package com.banking.gateway.util;

/**
 * Holds the current HTTP request's bearer token for the duration of that
 * request, so a gRPC ClientInterceptor (which has no direct access to the
 * HttpServletRequest) can attach it to outgoing gRPC calls as metadata.
 *
 * Known limitation: this relies on Spring MVC's synchronous, one-thread-per-
 * request model. It would need rework (e.g. Reactor context) if this
 * service ever moved to a reactive/async stack.
 */
public final class RequestTokenHolder {

    private static final ThreadLocal<String> CURRENT_TOKEN = new ThreadLocal<>();

    public static void set(String token) {
        CURRENT_TOKEN.set(token);
    }

    public static String get() {
        return CURRENT_TOKEN.get();
    }

    public static void clear() {
        CURRENT_TOKEN.remove();
    }

    private RequestTokenHolder() {}
}
