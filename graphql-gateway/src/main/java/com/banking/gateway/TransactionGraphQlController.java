package com.banking.gateway;

import com.banking.auth.grpc.AuthServiceGrpc;
import com.banking.auth.grpc.ValidateTokenRequest;
import com.banking.auth.grpc.ValidateTokenResponse;
import com.banking.gateway.dto.TransactionResult;
import com.banking.gateway.util.RequestTokenHolder;
import com.banking.transaction.grpc.*;
import io.grpc.Status;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class TransactionGraphQlController {

    @GrpcClient("transaction-service")
    private TransactionServiceGrpc.TransactionServiceBlockingStub transactionStub;

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

    @MutationMapping
    public TransactionResult transfer(
            @Argument String fromAccountId,
            @Argument String toAccountId,
            @Argument long amountMinorUnits,
            @Argument String currency,
            @Argument String idempotencyKey) {
        requireValidSession();

        String key = (idempotencyKey == null || idempotencyKey.isBlank())
            ? UUID.randomUUID().toString()
            : idempotencyKey;

        Transaction res = transactionStub.initiateTransfer(TransferRequest.newBuilder()
            .setFromAccountId(fromAccountId)
            .setToAccountId(toAccountId)
            .setAmountMinorUnits(amountMinorUnits)
            .setCurrency(currency)
            .setIdempotencyKey(key)
            .build());

        return toResult(res);
    }

    /**
     * The only way money enters the system at all — without this, every
     * account is permanently stuck at $0 and transfer() has nothing to
     * ever move, since it requires a source account with existing funds.
     */
    @MutationMapping
    public TransactionResult deposit(
            @Argument String toAccountId,
            @Argument long amountMinorUnits,
            @Argument String currency,
            @Argument String idempotencyKey) {
        requireValidSession();

        String key = (idempotencyKey == null || idempotencyKey.isBlank())
            ? UUID.randomUUID().toString()
            : idempotencyKey;

        Transaction res = transactionStub.initiateDeposit(DepositRequest.newBuilder()
            .setToAccountId(toAccountId)
            .setAmountMinorUnits(amountMinorUnits)
            .setCurrency(currency)
            .setIdempotencyKey(key)
            .build());

        return toResult(res);
    }

    @QueryMapping
    public TransactionResult transaction(@Argument String transactionId) {
        requireValidSession();
        Transaction res = transactionStub.getTransaction(GetTransactionRequest.newBuilder()
            .setTransactionId(transactionId)
            .build());
        return toResult(res);
    }

    @QueryMapping
    public List<TransactionResult> accountTransactions(@Argument String accountId) {
        requireValidSession();
        ListTransactionsResponse res = transactionStub.listTransactionsForAccount(
            ListTransactionsRequest.newBuilder().setAccountId(accountId).build()
        );
        return res.getTransactionsList().stream().map(this::toResult).collect(Collectors.toList());
    }

    private TransactionResult toResult(Transaction t) {
        return new TransactionResult(
            t.getTransactionId(),
            t.getFromAccountId(),
            t.getToAccountId(),
            t.getAmountMinorUnits(),
            t.getCurrency(),
            t.getStatus().name(),
            t.getIdempotencyKey(),
            t.getCreatedAt()
        );
    }
}
