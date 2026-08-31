package com.banking.gateway.interceptor;

import com.banking.gateway.util.RequestTokenHolder;
import io.grpc.*;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;

@GrpcGlobalClientInterceptor
public class AuthPropagationClientInterceptor implements ClientInterceptor {

    public static final Metadata.Key<String> AUTH_HEADER =
        Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                String token = RequestTokenHolder.get();
                if (token != null) {
                    headers.put(AUTH_HEADER, token);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
