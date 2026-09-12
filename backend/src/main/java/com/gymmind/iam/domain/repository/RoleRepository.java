package com.gymmind.iam.domain.repository;

import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import java.util.Optional;

public interface RoleRepository {
    Role save(Role role);
    Optional<Role> findByCode(RoleCode code);
    long count();
}
