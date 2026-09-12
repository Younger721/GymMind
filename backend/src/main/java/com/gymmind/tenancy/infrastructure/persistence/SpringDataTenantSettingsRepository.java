package com.gymmind.tenancy.infrastructure.persistence;

import com.gymmind.tenancy.domain.model.TenantSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface SpringDataTenantSettingsRepository extends JpaRepository<TenantSettings, Long> {

    Optional<TenantSettings> findByTenantId(Long tenantId);
}
