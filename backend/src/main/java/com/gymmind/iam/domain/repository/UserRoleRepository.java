package com.gymmind.iam.domain.repository;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserRole;

import java.util.Set;

public interface UserRoleRepository {
    UserRole save(UserRole userRole);
    void deleteByUserId(Long userId);
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
    long countByUserId(Long userId);
    Set<RoleCode> findRoleCodesByUserId(Long userId);
    Set<String> findPermissionCodesByUserId(Long userId);

    /**
     * Resolves effective permissions for a user in a tenant context. Legacy
     * implementations fall back to the global role permissions.
     */
    default Set<String> findPermissionCodesByUserId(Long userId, Long tenantId) {
        return findPermissionCodesByUserId(userId);
    }

    Set<Long> findUserIdsByRoleCodes(Set<RoleCode> roleCodes);
}
