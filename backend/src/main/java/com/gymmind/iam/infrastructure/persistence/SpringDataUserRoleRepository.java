package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserRoleRepository extends JpaRepository<UserRole, UserRole.Key> {
    boolean existsByUser_IdAndRole_Id(Long userId, Long roleId);
    long countByUser_Id(Long userId);
}
