package com.gymmind.ai.harness.infrastructure;

import com.gymmind.ai.harness.domain.TenantHarnessConfig;
import com.gymmind.ai.harness.domain.TenantHarnessConfigRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class JpaTenantHarnessConfigRepository implements TenantHarnessConfigRepository {

    private final SpringDataTenantHarnessConfigRepository delegate;

    JpaTenantHarnessConfigRepository(SpringDataTenantHarnessConfigRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public TenantHarnessConfig save(TenantHarnessConfig config) {
        return delegate.save(config);
    }

    @Override
    public Optional<TenantHarnessConfig> findByTenantId(Long tenantId) {
        return delegate.findById(tenantId);
    }
}
