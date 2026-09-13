package com.gymmind.tenancy.api.response;

import com.gymmind.tenancy.domain.model.Tenant;

public record TenantView(Long id, String code, String name, String status) {
    public static TenantView from(Tenant tenant) {
        return new TenantView(tenant.getId(), tenant.getCode(), tenant.getName(), tenant.getStatus().name());
    }
}
