package com.banking.account.grpc;

import com.banking.account.entity.AccountEntity;
import com.banking.account.repository.AccountJpaRepository;
import com.banking.account.util.GrpcContextKeys;
import com.banking.auth.grpc.AuthServiceGrpc;
import com.banking.auth.grpc.ValidateTokenRequest;
import com.banking.auth.grpc.ValidateTokenResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GrpcService
public class AccountGrpcService extends AccountServiceGrpc.AccountServiceImplBase {

    private final AccountJpaRepository accountRepository;

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authStub;

    public AccountGrpcService(AccountJpaRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    private record AuthenticatedCaller(UUID userId, List<String> roles) {}

    /**
     * This is the real Phase 4 proof point: account-service does not trust
     * any user_id a client claims — it takes the propagated token (read from
     * gRPC metadata by AuthPropagationServerInterceptor) and makes an actual
     * gRPC call to auth-service to verify it, every single request.
     */
    private AuthenticatedCaller requireValidUser() {
        String token = GrpcContextKeys.AUTH_TOKEN.get();
        if (token == null || token.isBlank()) {
            throw Status.UNAUTHENTICATED.withDescription("no auth token propagated").asRuntimeException();
        }

        ValidateTokenResponse response = authStub.validateToken(
            ValidateTokenRequest.newBuilder().setAccessToken(token).build()
        );

        if (!response.getValid()) {
            throw Status.UNAUTHENTICATED.withDescription("token rejected by auth-service").asRuntimeException();
        }

        return new AuthenticatedCaller(UUID.fromString(response.getUserId()), response.getRolesList());
    }

    private boolean isStaff(List<String> roles) {
        return roles.contains("teller") || roles.contains("admin");
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<Account> responseObserver) {
        AuthenticatedCaller caller = requireValidUser();

        // Deliberately ignore any user_id the client sent in the request —
        // the account belongs to whoever the verified token says they are,
        // never to a client-supplied claim.
        AccountEntity account = new AccountEntity(
            caller.userId(),
            request.getType().name(),
            request.getCurrency().isBlank() ? "USD" : request.getCurrency()
        );
        accountRepository.save(account);

        responseObserver.onNext(toProto(account));
        responseObserver.onCompleted();
    }

    @Override
    public void getAccount(GetAccountRequest request, StreamObserver<Account> responseObserver) {
        AuthenticatedCaller caller = requireValidUser();

        UUID accountId = UUID.fromString(request.getAccountId());
        AccountEntity account = accountRepository.findById(accountId)
            .orElseThrow(() -> Status.NOT_FOUND.withDescription("account not found").asRuntimeException());

        // Ownership check happens HERE, locally, because account-service is
        // the only service that actually has this data. auth-service's
        // Authorize only confirms role-level capability — see PHASE4_NOTES.md.
        boolean isOwner = account.getUserId().equals(caller.userId());
        if (!isOwner && !isStaff(caller.roles())) {
            throw Status.PERMISSION_DENIED.withDescription("not your account").asRuntimeException();
        }

        responseObserver.onNext(toProto(account));
        responseObserver.onCompleted();
    }

    @Override
    public void listAccountsForUser(ListAccountsRequest request, StreamObserver<ListAccountsResponse> responseObserver) {
        AuthenticatedCaller caller = requireValidUser();

        // Always returns the CALLER's own accounts — ignores any user_id in
        // the request. Staff viewing other customers' accounts on request is
        // a reasonable future addition, not implemented here.
        List<AccountEntity> accounts = accountRepository.findByUserId(caller.userId());

        ListAccountsResponse.Builder builder = ListAccountsResponse.newBuilder();
        accounts.forEach(a -> builder.addAccounts(toProto(a)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void updateAccountStatus(UpdateAccountStatusRequest request, StreamObserver<Account> responseObserver) {
        AuthenticatedCaller caller = requireValidUser();

        if (!isStaff(caller.roles())) {
            throw Status.PERMISSION_DENIED.withDescription("only staff can change account status").asRuntimeException();
        }

        UUID accountId = UUID.fromString(request.getAccountId());
        AccountEntity account = accountRepository.findById(accountId)
            .orElseThrow(() -> Status.NOT_FOUND.withDescription("account not found").asRuntimeException());

        account.setStatus(request.getStatus().name());
        accountRepository.save(account);

        responseObserver.onNext(toProto(account));
        responseObserver.onCompleted();
    }

    private Account toProto(AccountEntity entity) {
        return Account.newBuilder()
            .setAccountId(entity.getId().toString())
            .setUserId(entity.getUserId().toString())
            .setType(AccountType.valueOf(entity.getType()))
            .setStatus(AccountStatus.valueOf(entity.getStatus()))
            .setCurrency(entity.getCurrency())
            .setBalanceMinorUnits(entity.getBalanceMinorUnits())
            .setCreatedAt(DateTimeFormatter.ISO_INSTANT.format(entity.getCreatedAt()))
            .build();
    }
}
