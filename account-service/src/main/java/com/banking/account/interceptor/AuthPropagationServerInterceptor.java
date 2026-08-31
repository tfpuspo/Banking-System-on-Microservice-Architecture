package com.banking.account.interceptor;

import com.banking.account.util.GrpcContextKeys;
import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

/**
 * Reads the "authorization" metadata header off every incoming gRPC call
 * (set by graphql-gateway's AuthPropagationClientInterceptor) and makes it
 * available to the service implementation via gRPC Context — this is the
 * "auth-token propagation via metadata headers" piece of Phase 4.
 */
@GrpcGlobalServerInterceptor
public class AuthPropagationServerInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> AUTH_HEADER =
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        String token = headers.get(AUTH_HEADER);
        Context context = Context.current().withValue(GrpcContextKeys.AUTH_TOKEN, token);
        return Contexts.interceptCall(context, call, headers, next);
    }
}
