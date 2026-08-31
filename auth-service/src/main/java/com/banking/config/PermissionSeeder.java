package com.banking.auth.config;

import com.banking.auth.entity.RolePermissionEntity;
import com.banking.auth.repository.RolePermissionJpaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds a starting set of role permissions the first time auth-service runs
 * against an empty role_permissions table. Safe to run on every startup —
 * it checks the table is empty first, so it never duplicates or overwrites
 * rows an admin may have changed later.
 */
@Component
public class PermissionSeeder implements CommandLineRunner {

    private final RolePermissionJpaRepository permissionRepository;

    public PermissionSeeder(RolePermissionJpaRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Override
    public void run(String... args) {
        if (permissionRepository.count() > 0) {
            return; // already seeded, don't touch it
        }

        permissionRepository.saveAll(List.of(
            // admin: blanket access, no ownership checks
            new RolePermissionEntity("admin", "*", "*", false),

            // teller: can read/manage customer-facing resources, but isn't
            // restricted to their own — they're acting on customers' behalf
            new RolePermissionEntity("teller", "account", "read", false),
            new RolePermissionEntity("teller", "account", "freeze", false),
            new RolePermissionEntity("teller", "transaction", "read", false),

            // customer: can only act on resources that are their own
            new RolePermissionEntity("customer", "user", "read", true),
            new RolePermissionEntity("customer", "user", "update", true),
            new RolePermissionEntity("customer", "account", "read", true),
            new RolePermissionEntity("customer", "account", "transfer", true),
            new RolePermissionEntity("customer", "transaction", "read", true),
            new RolePermissionEntity("customer", "transaction", "create", true)
        ));
    }
}
