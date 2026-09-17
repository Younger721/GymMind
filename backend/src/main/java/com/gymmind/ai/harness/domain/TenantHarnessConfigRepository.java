package com.gymmind.ai.harness.domain;

import java.util.Optional;

public interface TenantHarnessConfigRepository {
    TenantHarnessConfig save(TenantHarnessConfig config);

    Optional<TenantHarnessConfig> findByTenantId(Long tenantId);
}
