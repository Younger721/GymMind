package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRolePermissionRepository extends JpaRepository<RolePermission, RolePermission.Key> {
    boolean existsById_RoleIdAndId_PermissionId(Long roleId, Long permissionId);
}
