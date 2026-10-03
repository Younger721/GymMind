package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.TenantRolePermission;
import com.gymmind.iam.domain.repository.TenantRolePermissionRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
class JpaTenantRolePermissionRepository implements TenantRolePermissionRepository {
    private final SpringDataTenantRolePermissionRepository delegate;

    JpaTenantRolePermissionRepository(SpringDataTenantRolePermissionRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public TenantRolePermission save(TenantRolePermission link) {
        return delegate.save(link);
    }

    @Override
    public Set<String> findPermissionCodesByTenantIdAndRoleId(Long tenantId, Long roleId) {
        return delegate.findPermissionCodesByTenantIdAndRoleId(tenantId, roleId);
    }

    @Override
    public Set<Long> findConfiguredRoleIdsByTenantId(Long tenantId) {
        return delegate.findConfiguredRoleIdsByTenantId(tenantId);
    }

    @Override
    public boolean existsByTenantIdAndRoleId(Long tenantId, Long roleId) {
        return delegate.existsConfiguration(tenantId, roleId);
    }

    @Override
    public void deleteByTenantIdAndRoleId(Long tenantId, Long roleId) {
        delegate.deleteById_TenantIdAndId_RoleId(tenantId, roleId);
    }
}
