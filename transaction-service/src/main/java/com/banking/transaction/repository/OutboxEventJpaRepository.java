package com.banking.transaction.repository;

import com.banking.transaction.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, java.util.UUID> {
    List<OutboxEventEntity> findTop50ByPublishedFalseOrderByCreatedAtAsc();
}
