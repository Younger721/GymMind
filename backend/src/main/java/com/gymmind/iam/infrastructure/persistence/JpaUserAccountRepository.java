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
    public Optional<UserAccount> findById(Long id) { return delegate.findById(id); }
    public Optional<UserAccount> findByNormalizedEmail(String email) { return delegate.findByNormalizedEmail(email); }
    public Optional<UserAccount> findByNormalizedUsername(String username) { return delegate.findByNormalizedUsername(username); }
    public Optional<UserAccount> findByTenantIdAndId(Long tenantId, Long id) { return delegate.findByTenantIdAndId(tenantId, id); }
    public Page<UserAccount> findAllByTenantId(Long tenantId, Pageable pageable) { return delegate.findAllByTenantId(tenantId, pageable); }
    public long countByTenantId(Long tenantId) { return delegate.countByTenantId(tenantId); }
    public Page<UserAccount> findAll(Pageable pageable) { return delegate.findAll(pageable); }
    public Page<UserAccount> findByTenantIdOrTenantIdIsNullOrderByIdAsc(Long tenantId, Pageable pageable) {
        return delegate.findByTenantIdOrTenantIdIsNullOrderByIdAsc(tenantId, pageable);
    }
    public java.util.List<UserAccount> findAll() { return delegate.findAll(); }
}
