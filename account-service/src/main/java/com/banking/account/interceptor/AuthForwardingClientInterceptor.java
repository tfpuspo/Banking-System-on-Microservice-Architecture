package com.banking.account.interceptor;

import com.banking.account.util.GrpcContextKeys;
import io.grpc.*;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;

/**
 * When account-service calls auth-service, this forwards whatever token was
 * propagated in from the original caller — completing the propagation
 * chain: browser → graphql-gateway → (metadata) → account-service →
 * (metadata) → auth-service.
 */
@GrpcGlobalClientInterceptor
public class AuthForwardingClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                String token = GrpcContextKeys.AUTH_TOKEN.get();
                if (token != null) {
                    headers.put(AuthPropagationServerInterceptor.AUTH_HEADER, token);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
