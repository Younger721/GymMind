package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface SpringDataPermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
}
