package com.gymmind.iam.domain.repository;

import com.gymmind.iam.domain.model.TenantRolePermission;

import java.util.Set;

public interface TenantRolePermissionRepository {
    TenantRolePermission save(TenantRolePermission link);

    Set<String> findPermissionCodesByTenantIdAndRoleId(Long tenantId, Long roleId);

    Set<Long> findConfiguredRoleIdsByTenantId(Long tenantId);

    boolean existsByTenantIdAndRoleId(Long tenantId, Long roleId);

    void deleteByTenantIdAndRoleId(Long tenantId, Long roleId);
}
