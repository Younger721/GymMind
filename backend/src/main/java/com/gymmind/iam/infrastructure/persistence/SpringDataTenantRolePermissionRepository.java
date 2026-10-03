package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.TenantRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

interface SpringDataTenantRolePermissionRepository
        extends JpaRepository<TenantRolePermission, TenantRolePermission.Key> {
    @Query("""
            select distinct link.permission.code
            from TenantRolePermission link
            where link.tenant.id = :tenantId and link.role.id = :roleId
            """)
    Set<String> findPermissionCodesByTenantIdAndRoleId(@Param("tenantId") Long tenantId,
                                                        @Param("roleId") Long roleId);

    @Query("""
            select distinct link.role.id
            from TenantRolePermission link
            where link.tenant.id = :tenantId
              and link.permission.code = 'role:menu:configured'
            """)
    Set<Long> findConfiguredRoleIdsByTenantId(@Param("tenantId") Long tenantId);

    @Query("""
            select case when count(link) > 0 then true else false end
            from TenantRolePermission link
            where link.tenant.id = :tenantId
              and link.role.id = :roleId
              and link.permission.code = 'role:menu:configured'
            """)
    boolean existsConfiguration(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

    void deleteById_TenantIdAndId_RoleId(Long tenantId, Long roleId);
}
