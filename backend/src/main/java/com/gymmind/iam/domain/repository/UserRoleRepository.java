package com.gymmind.iam.domain.repository;

import com.gymmind.iam.domain.model.UserRole;

public interface UserRoleRepository {
    UserRole save(UserRole userRole);
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
    long countByUserId(Long userId);
}
