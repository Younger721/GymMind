package com.gymmind.tenancy.domain.repository;

import com.gymmind.tenancy.domain.model.TenantSettings;

import java.util.Optional;

public interface TenantSettingsRepository {

    TenantSettings save(TenantSettings settings);

    Optional<TenantSettings> findByTenantId(Long tenantId);
}
