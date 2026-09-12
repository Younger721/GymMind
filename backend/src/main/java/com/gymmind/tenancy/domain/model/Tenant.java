package com.gymmind.tenancy.domain.model;

import com.gymmind.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.Locale;

@Entity
@Table(name = "sys_tenant", uniqueConstraints = @UniqueConstraint(name = "uk_sys_tenant_code", columnNames = "code"))
public class Tenant extends AuditableEntity {

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TenantStatus status;

    protected Tenant() {
    }

    private Tenant(String code, String name) {
        this.code = normalizeCode(code);
        this.name = requireName(name);
        this.status = TenantStatus.ACTIVE;
    }

    public static Tenant create(String code, String name) {
        return new Tenant(code, name);
    }

    public void activate() {
        status = TenantStatus.ACTIVE;
    }

    public void disable() {
        status = TenantStatus.DISABLED;
    }

    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    private static String normalizeCode(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Tenant code must not be blank");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Tenant code must not be blank");
        }
        return normalized;
    }

    private static String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tenant name must not be blank");
        }
        return value.trim();
    }
}
