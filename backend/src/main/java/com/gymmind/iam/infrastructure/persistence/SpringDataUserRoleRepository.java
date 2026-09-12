package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

interface SpringDataUserRoleRepository extends JpaRepository<UserRole, UserRole.Key> {
    boolean existsByUser_IdAndRole_Id(Long userId, Long roleId);
    long countByUser_Id(Long userId);

    @Query("""
            select distinct userRole.role.code
            from UserRole userRole
            where userRole.user.id = :userId
            """)
    Set<RoleCode> findRoleCodesByUserId(@Param("userId") Long userId);

    @Query("""
            select distinct rolePermission.permission.code
            from UserRole userRole, RolePermission rolePermission
            where userRole.user.id = :userId
              and rolePermission.role.id = userRole.role.id
            """)
    Set<String> findPermissionCodesByUserId(@Param("userId") Long userId);
}
