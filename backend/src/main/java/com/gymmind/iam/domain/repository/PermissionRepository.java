package com.gymmind.iam.domain.repository;

import com.gymmind.iam.domain.model.Permission;
import java.util.Optional;
import java.util.List;

public interface PermissionRepository {
    Permission save(Permission permission);
    Optional<Permission> findByCode(String code);
    List<Permission> findAll();
    long count();

    /**
     * Repairs legacy catalog rows without requiring Hibernate to hydrate a
     * malformed audit timestamp. Implementations may use a native update;
     * startup also performs the database-level repair before reads begin.
     */
    default void repairMalformedEntry(String code, String name) {
        // Legacy repository implementations do not need a second repair path.
    }
}
