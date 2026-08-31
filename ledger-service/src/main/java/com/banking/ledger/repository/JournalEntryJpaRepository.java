package com.banking.ledger.repository;

import com.banking.ledger.entity.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JournalEntryJpaRepository extends JpaRepository<JournalEntryEntity, UUID> {

    List<JournalEntryEntity> findByAccountIdOrderByCreatedAtDesc(UUID accountId);

    boolean existsByIdempotencyKeyAndAccountId(String idempotencyKey, UUID accountId);

    @Query("SELECT COALESCE(SUM(j.amountMinorUnits), 0) FROM JournalEntryEntity j WHERE j.accountId = :accountId")
    long sumBalanceForAccount(@Param("accountId") UUID accountId);
}
