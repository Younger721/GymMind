package com.gymmind.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class TenantScopedEntity extends AuditableEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    protected TenantScopedEntity() {
    }

    protected TenantScopedEntity(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getTenantId() {
        return tenantId;
    }
}
