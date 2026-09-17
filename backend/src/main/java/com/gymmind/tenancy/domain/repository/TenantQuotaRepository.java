package com.gymmind.tenancy.domain.repository;

import com.gymmind.tenancy.domain.model.TenantQuota;

import java.util.Optional;

public interface TenantQuotaRepository {
    TenantQuota save(TenantQuota quota);

    Optional<TenantQuota> findByTenantId(Long tenantId);
}
