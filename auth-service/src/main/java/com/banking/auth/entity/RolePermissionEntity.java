package com.banking.auth.entity;

import jakarta.persistence.*;

/**
 * A single grant: "this role may perform this action on this resource type."
 *
 * Using "*" for resourceType or action means "any" — this is how the admin
 * role gets blanket access without needing one row per resource/action pair.
 *
 * ownershipRequired: when true, the grant only applies if the resource ID in
 * the Authorize request matches the requesting user's own ID (e.g. a
 * customer reading their own profile). This is currently only enforceable
 * for resourceType "user", since account/transaction ownership can't be
 * verified until account-service and transaction-service exist (Phase 6) —
 * see AuthGrpcService.authorize() for how that's handled honestly in the
 * meantime, rather than silently trusting an unverifiable claim.
 */
@Entity
@Table(name = "role_permissions")
public class RolePermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String role;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(nullable = false)
    private String action;

    @Column(name = "ownership_required", nullable = false)
    private boolean ownershipRequired;

    protected RolePermissionEntity() {
        // required by JPA
    }

    public RolePermissionEntity(String role, String resourceType, String action, boolean ownershipRequired) {
        this.role = role;
        this.resourceType = resourceType;
        this.action = action;
        this.ownershipRequired = ownershipRequired;
    }

    public Long getId() { return id; }
    public String getRole() { return role; }
    public String getResourceType() { return resourceType; }
    public String getAction() { return action; }
    public boolean isOwnershipRequired() { return ownershipRequired; }
}
