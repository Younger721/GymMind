package com.gymmind.tenancy.application.port;

import com.gymmind.tenancy.domain.model.Tenant;

@FunctionalInterface
public interface TenantProvisioningContributor {
    void contribute(Tenant tenant);
}
