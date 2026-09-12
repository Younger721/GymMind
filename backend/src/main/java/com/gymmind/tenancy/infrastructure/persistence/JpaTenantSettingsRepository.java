package com.gymmind.tenancy.infrastructure.persistence;

import com.gymmind.tenancy.domain.model.TenantSettings;
import com.gymmind.tenancy.domain.repository.TenantSettingsRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaTenantSettingsRepository implements TenantSettingsRepository {

    private final SpringDataTenantSettingsRepository delegate;

    JpaTenantSettingsRepository(SpringDataTenantSettingsRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public TenantSettings save(TenantSettings settings) {
        return delegate.save(settings);
    }

    @Override
    public Optional<TenantSettings> findByTenantId(Long tenantId) {
        return delegate.findByTenantId(tenantId);
    }
}
