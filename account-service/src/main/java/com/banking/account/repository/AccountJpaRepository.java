package com.banking.account.repository;

import com.banking.account.entity.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, UUID> {
    List<AccountEntity> findByUserId(UUID userId);
}
