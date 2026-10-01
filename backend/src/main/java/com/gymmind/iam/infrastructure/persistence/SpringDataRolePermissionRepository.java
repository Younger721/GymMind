package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRolePermissionRepository extends JpaRepository<RolePermission, RolePermission.Key> {
    boolean existsById_RoleIdAndId_PermissionId(Long roleId, Long permissionId);
    @org.springframework.data.jpa.repository.Query("select rp.permission.code from RolePermission rp where rp.role.id = :roleId")
    java.util.Set<String> findPermissionCodesByRoleId(@org.springframework.data.repository.query.Param("roleId") Long roleId);
    void deleteById_RoleId(Long roleId);
}
