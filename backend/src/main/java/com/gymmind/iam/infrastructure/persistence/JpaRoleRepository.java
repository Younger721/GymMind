package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.Role;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.repository.RoleRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
class JpaRoleRepository implements RoleRepository {
    private final SpringDataRoleRepository delegate;
    JpaRoleRepository(SpringDataRoleRepository delegate) { this.delegate = delegate; }
    public Role save(Role role) { return delegate.save(role); }
    public Optional<Role> findByCode(RoleCode code) { return delegate.findByCode(code); }
    public long count() { return delegate.count(); }
}
