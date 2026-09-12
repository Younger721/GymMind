package com.gymmind.tenancy.infrastructure.persistence;

import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.repository.TenantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaTenantRepository implements TenantRepository {

    private final SpringDataTenantRepository delegate;

    JpaTenantRepository(SpringDataTenantRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public Tenant save(Tenant tenant) {
        return delegate.save(tenant);
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return delegate.findById(id);
    }

    @Override
    public Optional<Tenant> findByCode(String normalizedCode) {
        return delegate.findByCode(normalizedCode);
    }

    @Override
    public boolean existsByCode(String normalizedCode) {
        return delegate.existsByCode(normalizedCode);
    }

    @Override
    public Page<Tenant> findAll(Pageable pageable) {
        return delegate.findAll(pageable);
    }
}
