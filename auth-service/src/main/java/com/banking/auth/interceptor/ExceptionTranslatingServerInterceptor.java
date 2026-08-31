package com.banking.auth.interceptor;

import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Without this, unexpected exceptions fall through to gRPC's generic
 * handler, which returns the unhelpful "Application error processing RPC"
 * message hit earlier in this project (the Register bug during Phase 2).
 * This catches it first and returns a real Status with a useful description.
 */
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
                    call.close(
                        Status.INTERNAL.withDescription("Internal error: " + e.getMessage()),
                        new Metadata()
                    );
                }
            }
        };
    }
}
