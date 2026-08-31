package com.banking.transaction.grpc;

import com.banking.auth.grpc.AuthServiceGrpc;
import com.banking.auth.grpc.ValidateTokenRequest;
import com.banking.auth.grpc.ValidateTokenResponse;
import com.banking.transaction.entity.OutboxEventEntity;
import com.banking.transaction.entity.TransactionEntity;
import com.banking.transaction.kafka.KafkaTopics;
import com.banking.transaction.kafka.dto.DepositRequestedEvent;
import com.banking.transaction.kafka.dto.TransferRequestedEvent;
import com.banking.transaction.repository.OutboxEventJpaRepository;
import com.banking.transaction.repository.TransactionJpaRepository;
import com.banking.transaction.util.GrpcContextKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@GrpcService
public class TransactionGrpcService extends TransactionServiceGrpc.TransactionServiceImplBase {

    private final TransactionJpaRepository transactionRepository;
    private final OutboxEventJpaRepository outboxRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GrpcClient("auth-service")
    private AuthServiceGrpc.AuthServiceBlockingStub authStub;

    public TransactionGrpcService(TransactionJpaRepository transactionRepository, OutboxEventJpaRepository outboxRepository) {
        this.transactionRepository = transactionRepository;
        this.outboxRepository = outboxRepository;
    }

    private void requireValidSession() {
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
    }

    @Override
    @Transactional
    public void initiateTransfer(TransferRequest request, StreamObserver<Transaction> responseObserver) {
        requireValidSession();

        Optional<TransactionEntity> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            responseObserver.onNext(toProto(existing.get()));
            responseObserver.onCompleted();
            return;
        }

        UUID fromAccountId = UUID.fromString(request.getFromAccountId());
        UUID toAccountId = UUID.fromString(request.getToAccountId());

        TransactionEntity transaction = new TransactionEntity(
            fromAccountId, toAccountId, request.getAmountMinorUnits(), request.getCurrency(), request.getIdempotencyKey()
        );
        transaction.setType("TRANSFER");
        transactionRepository.save(transaction);

        TransferRequestedEvent event = new TransferRequestedEvent(
            transaction.getId().toString(),
            fromAccountId.toString(),
            toAccountId.toString(),
            transaction.getAmountMinorUnits(),
            transaction.getCurrency(),
            transaction.getIdempotencyKey()
        );

        writeOutboxEvent("TransferRequested", event);

        responseObserver.onNext(toProto(transaction));
        responseObserver.onCompleted();
    }

    /**
     * Same Outbox pattern as InitiateTransfer: transaction row + outbox
     * event, one atomic write. The only structural difference from a
     * transfer is there's no fromAccountId — ledger-service credits the
     * destination with no matching debit anywhere (money enters the system
     * here, rather than moving between two existing accounts).
     */
    @Override
    @Transactional
    public void initiateDeposit(DepositRequest request, StreamObserver<Transaction> responseObserver) {
        requireValidSession();

        Optional<TransactionEntity> existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            responseObserver.onNext(toProto(existing.get()));
            responseObserver.onCompleted();
            return;
        }

        UUID toAccountId = UUID.fromString(request.getToAccountId());

        TransactionEntity transaction = new TransactionEntity(
            null, toAccountId, request.getAmountMinorUnits(), request.getCurrency(), request.getIdempotencyKey()
        );
        transaction.setType("DEPOSIT");
        transactionRepository.save(transaction);

        DepositRequestedEvent event = new DepositRequestedEvent(
            transaction.getId().toString(),
            toAccountId.toString(),
            transaction.getAmountMinorUnits(),
            transaction.getCurrency(),
            transaction.getIdempotencyKey()
        );

        writeOutboxEvent("DepositRequested", event);

        responseObserver.onNext(toProto(transaction));
        responseObserver.onCompleted();
    }

    @Override
    public void getTransaction(GetTransactionRequest request, StreamObserver<Transaction> responseObserver) {
        requireValidSession();
        UUID id = UUID.fromString(request.getTransactionId());
        TransactionEntity transaction = transactionRepository.findById(id)
            .orElseThrow(() -> Status.NOT_FOUND.withDescription("transaction not found").asRuntimeException());
        responseObserver.onNext(toProto(transaction));
        responseObserver.onCompleted();
    }

    @Override
    public void listTransactionsForAccount(ListTransactionsRequest request, StreamObserver<ListTransactionsResponse> responseObserver) {
        requireValidSession();
        UUID accountId = UUID.fromString(request.getAccountId());
        List<TransactionEntity> transactions = transactionRepository.findByAccountId(accountId);

        ListTransactionsResponse.Builder builder = ListTransactionsResponse.newBuilder();
        transactions.forEach(t -> builder.addTransactions(toProto(t)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private void writeOutboxEvent(String eventType, Object event) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            throw Status.INTERNAL.withDescription("failed to serialize event").asRuntimeException();
        }
        outboxRepository.save(new OutboxEventEntity(KafkaTopics.LEDGER_COMMANDS, eventType, payload));
    }

    private Transaction toProto(TransactionEntity entity) {
        Transaction.Builder builder = Transaction.newBuilder()
            .setTransactionId(entity.getId().toString())
            .setAmountMinorUnits(entity.getAmountMinorUnits())
            .setCurrency(entity.getCurrency())
            .setStatus(TransactionStatus.valueOf(entity.getStatus()))
            .setCreatedAt(entity.getCreatedAt().toString())
            .setIdempotencyKey(entity.getIdempotencyKey());

        if (entity.getFromAccountId() != null) builder.setFromAccountId(entity.getFromAccountId().toString());
        if (entity.getToAccountId() != null) builder.setToAccountId(entity.getToAccountId().toString());

        return builder.build();
    }
}
