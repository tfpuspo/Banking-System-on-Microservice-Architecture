package com.banking.account.interceptor;

import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Without this, any unexpected exception (a bug, a bad null, whatever) falls
 * through to gRPC's own generic handler, which just returns the unhelpful
 * "Application error processing RPC" message we hit earlier in this project.
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
                    // already a deliberate, well-formed error (e.g. Status.UNAUTHENTICATED) — let it through as-is
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
