package com.banking.transaction.interceptor;

import com.banking.transaction.util.GrpcContextKeys;
import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

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
