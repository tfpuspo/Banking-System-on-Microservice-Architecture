package com.banking.auth.repository;

import com.banking.auth.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, Long> {

    long count(); // used by the seeder to check if defaults already exist

    List<RolePermissionEntity> findByRoleIn(Collection<String> roles);
}
