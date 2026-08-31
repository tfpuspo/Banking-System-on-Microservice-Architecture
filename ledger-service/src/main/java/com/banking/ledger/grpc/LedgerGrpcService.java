package com.banking.ledger.grpc;

import com.banking.ledger.entity.JournalEntryEntity;
import com.banking.ledger.repository.JournalEntryJpaRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.UUID;

/**
 * PostEntry is deliberately left unimplemented here (it defaults to
 * UNIMPLEMENTED via the generated base class). The proto defines it as a
 * direct synchronous posting API, but the actual transfer flow in this
 * system posts entries via the Kafka-driven Saga instead
 * (TransferCommandConsumer) — exactly per Phase 6's instruction to avoid
 * naive synchronous multi-service calls for money movement. PostEntry
 * stays in the contract for a possible future direct-posting use case.
 */
@GrpcService
public class LedgerGrpcService extends LedgerServiceGrpc.LedgerServiceImplBase {

    private final JournalEntryJpaRepository journalRepository;

    public LedgerGrpcService(JournalEntryJpaRepository journalRepository) {
        this.journalRepository = journalRepository;
    }

    @Override
    public void getBalance(GetBalanceRequest request, StreamObserver<GetBalanceResponse> responseObserver) {
        UUID accountId = UUID.fromString(request.getAccountId());
        long balance = journalRepository.sumBalanceForAccount(accountId);

        responseObserver.onNext(GetBalanceResponse.newBuilder()
            .setAccountId(request.getAccountId())
            .setBalanceMinorUnits(balance)
            .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getJournal(GetJournalRequest request, StreamObserver<GetJournalResponse> responseObserver) {
        UUID accountId = UUID.fromString(request.getAccountId());
        List<JournalEntryEntity> entries = journalRepository.findByAccountIdOrderByCreatedAtDesc(accountId);

        GetJournalResponse.Builder builder = GetJournalResponse.newBuilder();
        for (JournalEntryEntity e : entries) {
            builder.addEntries(JournalEntry.newBuilder()
                .setEntryId(e.getId().toString())
                .setTransactionId(e.getTransactionId().toString())
                .setAccountId(e.getAccountId().toString())
                .setAmountMinorUnits(e.getAmountMinorUnits())
                .setCreatedAt(e.getCreatedAt().toString())
                .build());
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
