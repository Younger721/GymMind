package com.gymmind.tenancy.application;

import com.gymmind.tenancy.application.port.TenantProvisioningContributor;
import com.gymmind.tenancy.domain.model.Tenant;
import com.gymmind.tenancy.domain.model.TenantSettings;
import com.gymmind.tenancy.domain.repository.TenantSettingsRepository;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@Order(100)
public class TenantSettingsProvisioningContributor implements TenantProvisioningContributor {
    private final TenantSettingsRepository settingsRepository;

    public TenantSettingsProvisioningContributor(TenantSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    public void contribute(Tenant tenant) {
        if (settingsRepository.findByTenantId(tenant.getId()).isEmpty()) {
            settingsRepository.save(TenantSettings.create(tenant.getId()));
        }
    }
}
