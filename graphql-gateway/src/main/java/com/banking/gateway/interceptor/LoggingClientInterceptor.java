package com.banking.gateway.interceptor;

import io.grpc.*;
import net.devh.boot.grpc.client.interceptor.GrpcGlobalClientInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcGlobalClientInterceptor
public class LoggingClientInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingClientInterceptor.class);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        long start = System.currentTimeMillis();
        String methodName = method.getFullMethodName();

        ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);
        return new ForwardingClientCall.SimpleForwardingClientCall<>(call) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        long durationMs = System.currentTimeMillis() - start;
                        log.info("[gRPC out] {} -> {} ({}ms)", methodName, status.getCode(), durationMs);
                        super.onClose(status, trailers);
                    }
                }, headers);
            }
        };
    }
}
