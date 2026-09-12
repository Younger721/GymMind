package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserRole;
import com.gymmind.iam.domain.repository.UserRoleRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
class JpaUserRoleRepository implements UserRoleRepository {
    private final SpringDataUserRoleRepository delegate;

    JpaUserRoleRepository(SpringDataUserRoleRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public UserRole save(UserRole userRole) {
        return delegate.save(userRole);
    }

    @Override
    public boolean existsByUserIdAndRoleId(Long userId, Long roleId) {
        return delegate.existsByUser_IdAndRole_Id(userId, roleId);
    }

    @Override
    public long countByUserId(Long userId) {
        return delegate.countByUser_Id(userId);
    }

    @Override
    public Set<RoleCode> findRoleCodesByUserId(Long userId) {
        return delegate.findRoleCodesByUserId(userId);
    }

    @Override
    public Set<String> findPermissionCodesByUserId(Long userId) {
        return delegate.findPermissionCodesByUserId(userId);
    }
}
