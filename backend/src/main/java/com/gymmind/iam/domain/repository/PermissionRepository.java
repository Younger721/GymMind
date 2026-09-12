package com.gymmind.iam.domain.repository;

import com.gymmind.iam.domain.model.Permission;
import java.util.Optional;

public interface PermissionRepository {
    Permission save(Permission permission);
    Optional<Permission> findByCode(String code);
}
