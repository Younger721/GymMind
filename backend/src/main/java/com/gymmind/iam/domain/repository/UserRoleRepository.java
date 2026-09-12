package com.gymmind.iam.domain.repository;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserRole;

import java.util.Set;

public interface UserRoleRepository {
    UserRole save(UserRole userRole);
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
    long countByUserId(Long userId);
    Set<RoleCode> findRoleCodesByUserId(Long userId);
    Set<String> findPermissionCodesByUserId(Long userId);
}
