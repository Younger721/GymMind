package com.gymmind.shared.security;

import com.gymmind.iam.domain.model.RoleCode;

import java.util.Set;

public record CurrentActor(
        Long userId,
        Long tenantId,
        Set<RoleCode> roles,
        Set<String> permissions,
        long tokenVersion,
        String tokenId) {

    public CurrentActor {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
        if (tenantId != null && tenantId <= 0) {
            throw new IllegalArgumentException("Tenant id must be positive");
        }
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Roles must not be empty");
        }
        roles = Set.copyOf(roles);
        if (tenantId == null && !roles.equals(Set.of(RoleCode.PLATFORM_ADMIN))) {
            throw new IllegalArgumentException("Platform actor must have only the platform administrator role");
        }
        if (tenantId != null && roles.contains(RoleCode.PLATFORM_ADMIN)) {
            throw new IllegalArgumentException("Tenant actor must not have the platform administrator role");
        }
        if (permissions == null) {
            throw new IllegalArgumentException("Permissions must not be null");
        }
        permissions = Set.copyOf(permissions);
        if (permissions.stream().anyMatch(permission -> permission.isBlank())) {
            throw new IllegalArgumentException("Permission must not be blank");
        }
        if (tokenVersion < 0) {
            throw new IllegalArgumentException("Token version must not be negative");
        }
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("Token id must not be blank");
        }
        tokenId = tokenId.trim();
    }

    public boolean isPlatformAdmin() {
        return roles.contains(RoleCode.PLATFORM_ADMIN);
    }

    public boolean hasPermission(String permission) {
        return permission != null && permissions.contains(permission);
    }
}
