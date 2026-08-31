package com.banking.gateway;

import com.banking.auth.grpc.AuthServiceGrpc;
import com.banking.auth.grpc.ValidateTokenRequest;
import com.banking.auth.grpc.ValidateTokenResponse;
import com.banking.gateway.util.RequestTokenHolder;
import com.banking.ledger.grpc.GetBalanceRequest;
import com.banking.ledger.grpc.GetBalanceResponse;
import com.banking.ledger.grpc.LedgerServiceGrpc;
import io.grpc.Status;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * account-service's `balanceMinorUnits` column is a leftover placeholder —
 * nothing writes to it after a transfer completes, since ledger-service
 * (via the double-entry journal) is the actual source of truth for money
 * movement, by design (see PHASE1 SERVICES.md, PHASE6_NOTES.md). This
 * exposes the REAL balance, computed from the journal, so the frontend
 * shows accurate numbers rather than a permanently-stale $0.00.
 */
@Controller
public class LedgerGraphQlController {

    @GrpcClient("ledger-service")
    private LedgerServiceGrpc.LedgerServiceBlockingStub ledgerStub;

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authStub;

    private void requireValidSession() {
        String token = RequestTokenHolder.get();
        if (token == null || token.isBlank()) {
            throw Status.UNAUTHENTICATED
                .withDescription("Missing Authorization header — this operation requires a signed-in session.")
                .asRuntimeException();
        }
        ValidateTokenResponse validation = authStub.validateToken(
            ValidateTokenRequest.newBuilder().setAccessToken(token).build()
        );
        if (!validation.getValid()) {
            throw Status.UNAUTHENTICATED
                .withDescription("Session is invalid or expired — please sign in again.")
                .asRuntimeException();
        }
    }

    @QueryMapping
    public long accountBalance(@Argument String accountId) {
        requireValidSession();
        GetBalanceResponse res = ledgerStub.getBalance(GetBalanceRequest.newBuilder()
            .setAccountId(accountId)
            .build());
        return res.getBalanceMinorUnits();
    }
}
