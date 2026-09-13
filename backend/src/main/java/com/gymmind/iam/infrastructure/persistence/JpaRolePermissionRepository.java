package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.RolePermission;
import com.gymmind.iam.domain.repository.RolePermissionRepository;
import org.springframework.stereotype.Repository;

@Repository
class JpaRolePermissionRepository implements RolePermissionRepository {
    private final SpringDataRolePermissionRepository delegate;

    JpaRolePermissionRepository(SpringDataRolePermissionRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public RolePermission save(RolePermission link) {
        return delegate.save(link);
    }

    @Override
    public boolean exists(Long roleId, Long permissionId) {
        return delegate.existsById_RoleIdAndId_PermissionId(roleId, permissionId);
    }
}
