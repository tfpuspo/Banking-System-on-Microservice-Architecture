package com.banking.transaction.interceptor;

import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcGlobalServerInterceptor
public class ExceptionTranslatingServerInterceptor implements ServerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ExceptionTranslatingServerInterceptor.class);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(listener) {
            @Override
            public void onHalfClose() {
                try {
                    super.onHalfClose();
                } catch (StatusRuntimeException e) {
                    call.close(e.getStatus(), new Metadata());
                } catch (Exception e) {
                    log.error("Unhandled exception in {}", call.getMethodDescriptor().getFullMethodName(), e);
                    call.close(Status.INTERNAL.withDescription("Internal error: " + e.getMessage()), new Metadata());
                }
            }
        };
    }
}
