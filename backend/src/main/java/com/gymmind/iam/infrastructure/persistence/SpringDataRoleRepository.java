package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

interface SpringDataRoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(RoleCode code);
}
