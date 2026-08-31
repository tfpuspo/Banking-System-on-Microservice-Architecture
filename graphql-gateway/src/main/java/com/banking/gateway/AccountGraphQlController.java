package com.banking.gateway;

import com.banking.account.grpc.*;
import com.banking.auth.grpc.AuthServiceGrpc;
import com.banking.auth.grpc.ValidateTokenRequest;
import com.banking.auth.grpc.ValidateTokenResponse;
import com.banking.gateway.dto.AccountResult;
import com.banking.gateway.util.RequestTokenHolder;
import io.grpc.Status;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AccountGraphQlController {

    @GrpcClient("account-service")
    private AccountServiceGrpc.AccountServiceBlockingStub accountStub;

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authStub;

    /**
     * Wires auth into the gateway (Phase 5): every protected operation
     * checks a real, verified session BEFORE calling account-service at
     * all. Throwing a StatusRuntimeException here is deliberate — our
     * existing GrpcExceptionResolver (built in Phase 2) already converts
     * these into clean GraphQL errors, so this reuses proven machinery
     * instead of introducing new, unverified Spring GraphQL APIs.
     *
     * account-service still verifies the token itself too — defense in
     * depth, not redundancy. See PHASE5_NOTES.md.
     */
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

    @MutationMapping
    public AccountResult createAccount(@Argument String type, @Argument String currency) {
        requireValidSession();
        Account res = accountStub.createAccount(CreateAccountRequest.newBuilder()
            .setType(AccountType.valueOf(type))
            .setCurrency(currency)
            .build());
        return toResult(res);
    }

    @QueryMapping
    public AccountResult account(@Argument String accountId) {
        requireValidSession();
        Account res = accountStub.getAccount(GetAccountRequest.newBuilder()
            .setAccountId(accountId)
            .build());
        return toResult(res);
    }

    @QueryMapping
    public List<AccountResult> myAccounts() {
        requireValidSession();
        ListAccountsResponse res = accountStub.listAccountsForUser(ListAccountsRequest.newBuilder().build());
        return res.getAccountsList().stream().map(this::toResult).collect(Collectors.toList());
    }

    /**
     * account-service itself already restricts this to teller/admin roles
     * (see AccountGrpcService.updateAccountStatus) — this resolver just
     * exposes that existing gRPC call through GraphQL. A customer calling
     * this gets a PERMISSION_DENIED error from account-service, same as
     * any other protection in this system: enforced at the service that
     * owns the data, not just hidden in the UI.
     */
    @MutationMapping
    public AccountResult updateAccountStatus(@Argument String accountId, @Argument String status) {
        requireValidSession();
        Account res = accountStub.updateAccountStatus(UpdateAccountStatusRequest.newBuilder()
            .setAccountId(accountId)
            .setStatus(AccountStatus.valueOf(status))
            .build());
        return toResult(res);
    }

    private AccountResult toResult(Account a) {
        return new AccountResult(
            a.getAccountId(),
            a.getUserId(),
            a.getType().name(),
            a.getStatus().name(),
            a.getCurrency(),
            a.getBalanceMinorUnits(),
            a.getCreatedAt()
        );
    }
}
