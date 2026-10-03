package com.gymmind.tenancy.api.response;

import com.gymmind.tenancy.domain.model.Tenant;

import java.time.Instant;

public record TenantView(Long id, String code, String name, String status, Instant createdAt, long userCount) {
    public TenantView(Long id, String code, String name, String status) {
        this(id, code, name, status, null, 0L);
    }

    public static TenantView from(Tenant tenant) {
        return from(tenant, 0L);
    }

    public static TenantView from(Tenant tenant, long userCount) {
        return new TenantView(tenant.getId(), tenant.getCode(), tenant.getName(), tenant.getStatus().name(),
                tenant.getCreatedAt(), userCount);
    }
}
