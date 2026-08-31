package com.banking.account.interceptor;

import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcGlobalServerInterceptor
public class LoggingServerInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingServerInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        long start = System.currentTimeMillis();
        String methodName = call.getMethodDescriptor().getFullMethodName();

        ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                long durationMs = System.currentTimeMillis() - start;
                log.info("[gRPC] {} -> {} ({}ms)", methodName, status.getCode(), durationMs);
                super.close(status, trailers);
            }
        };

        return next.startCall(wrappedCall, headers);
    }
}
