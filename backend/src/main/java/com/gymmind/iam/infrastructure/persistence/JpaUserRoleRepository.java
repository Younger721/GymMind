package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserRole;
import com.gymmind.platform.bootstrap.PermissionCatalog;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
class JpaUserRoleRepository implements UserRoleRepository {
    private final SpringDataUserRoleRepository delegate;
    private final SpringDataRolePermissionRepository globalPermissions;
    private final SpringDataTenantRolePermissionRepository tenantPermissions;

    JpaUserRoleRepository(SpringDataUserRoleRepository delegate,
                          SpringDataRolePermissionRepository globalPermissions,
                          SpringDataTenantRolePermissionRepository tenantPermissions) {
        this.delegate = delegate;
        this.globalPermissions = globalPermissions;
        this.tenantPermissions = tenantPermissions;
    }

    @Override
    public UserRole save(UserRole userRole) {
        return delegate.save(userRole);
    }

    @Override
    public void deleteByUserId(Long userId) {
        delegate.deleteByUser_Id(userId);
    }

    @Override
    public boolean existsByUserIdAndRoleId(Long userId, Long roleId) {
        return delegate.existsByUser_IdAndRole_Id(userId, roleId);
    }

    @Override
    public long countByUserId(Long userId) {
        return delegate.countByUser_Id(userId);
    }

    @Override
    public Set<RoleCode> findRoleCodesByUserId(Long userId) {
        return delegate.findRoleCodesByUserId(userId);
    }

    @Override
    public Set<String> findPermissionCodesByUserId(Long userId) {
        Set<String> resolved = new java.util.HashSet<>();
        for (RoleCode roleCode : delegate.findRoleCodesByUserId(userId)) {
            resolved.addAll(allowedPermissionCodes(roleCode, globalPermissions.findPermissionCodesByRoleId(
                    delegate.findRoleIdByUserIdAndRoleCode(userId, roleCode))));
        }
        return Set.copyOf(resolved);
    }

    @Override
    public Set<String> findPermissionCodesByUserId(Long userId, Long tenantId) {
        if (tenantId == null) {
            return findPermissionCodesByUserId(userId);
        }
        Set<Long> roleIds = delegate.findActiveRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> configuredRoleIds = tenantPermissions.findConfiguredRoleIdsByTenantId(tenantId);
        java.util.Set<String> resolved = new java.util.HashSet<>();
        for (Long roleId : roleIds) {
            Set<String> codes = configuredRoleIds.contains(roleId)
                    ? tenantPermissions.findPermissionCodesByTenantIdAndRoleId(tenantId, roleId)
                    : globalPermissions.findPermissionCodesByRoleId(roleId);
            if (codes != null) {
                resolved.addAll(allowedPermissionCodes(roleCodeForId(roleId), codes));
            }
        }
        return Set.copyOf(resolved);
    }

    private RoleCode roleCodeForId(Long roleId) {
        return delegate.findRoleCodeById(roleId);
    }

    private static Set<String> allowedPermissionCodes(RoleCode roleCode, Set<String> codes) {
        if (roleCode == RoleCode.PLATFORM_ADMIN) return codes == null ? Set.of() : codes;
        Set<String> allowed = new java.util.HashSet<>(PermissionCatalog.defaultMenus(roleCode));
        PermissionCatalog.defaultMenus(roleCode).forEach(menu -> allowed.addAll(
                PermissionCatalog.operationPermissionsFor(roleCode, menu)));
        allowed.add(PermissionCatalog.MENU_CONFIGURED_MARKER);
        return codes == null ? Set.of() : codes.stream().filter(allowed::contains).collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public Set<Long> findUserIdsByRoleCodes(Set<RoleCode> roleCodes) {
        return delegate.findUserIdsByRoleCodes(roleCodes);
    }
}
