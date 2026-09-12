package com.gymmind.iam.infrastructure.persistence;

import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.iam.domain.repository.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
class JpaUserAccountRepository implements UserAccountRepository {
    private final SpringDataUserAccountRepository delegate;
    JpaUserAccountRepository(SpringDataUserAccountRepository delegate) { this.delegate = delegate; }
    public UserAccount save(UserAccount user) { return delegate.save(user); }
    public Optional<UserAccount> findByNormalizedEmail(String email) { return delegate.findByNormalizedEmail(email); }
    public Optional<UserAccount> findByTenantIdAndId(Long tenantId, Long id) { return delegate.findByTenantIdAndId(tenantId, id); }
    public Page<UserAccount> findAllByTenantId(Long tenantId, Pageable pageable) { return delegate.findAllByTenantId(tenantId, pageable); }
}
