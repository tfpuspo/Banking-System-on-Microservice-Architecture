package com.banking.transaction.repository;

import com.banking.transaction.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {
    Optional<TransactionEntity> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM TransactionEntity t WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId ORDER BY t.createdAt DESC")
    List<TransactionEntity> findByAccountId(@Param("accountId") UUID accountId);
}
