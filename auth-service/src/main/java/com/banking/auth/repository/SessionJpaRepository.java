package com.banking.auth.repository;

import com.banking.auth.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessionJpaRepository extends JpaRepository<SessionEntity, UUID> {
    Optional<SessionEntity> findByRefreshToken(String refreshToken);
}
