package com.gymmind.tenancy.application;

import com.gymmind.iam.domain.model.UserAccount;
import com.gymmind.tenancy.domain.model.Tenant;

public interface TenantProvisioningService {
    ProvisionedTenant provision(Tenant tenant, Administrator administrator);

    record Administrator(String email, String passwordHash, String displayName) {}

    record ProvisionedTenant(Tenant tenant, UserAccount administrator) {}
}
