package com.gymmind.iam.domain.repository;

import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import java.util.Optional;
import java.util.List;

public interface RoleRepository {
    Role save(Role role);
    Optional<Role> findByCode(RoleCode code);
    List<Role> findAll();
    long count();
}
