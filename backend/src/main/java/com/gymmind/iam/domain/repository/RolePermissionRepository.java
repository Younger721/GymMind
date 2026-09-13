package com.gymmind.iam.domain.repository;

import com.gymmind.iam.domain.model.RolePermission;

public interface RolePermissionRepository {
    RolePermission save(RolePermission link);
    boolean exists(Long roleId, Long permissionId);
}
