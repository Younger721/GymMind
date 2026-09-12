package com.gymmind.iam.domain.model;

import com.gymmind.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "sys_permission", uniqueConstraints = @UniqueConstraint(name = "uk_sys_permission_code", columnNames = "code"))
public class Permission extends AuditableEntity {
    @Column(nullable = false, length = 128)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    protected Permission() {}

    private Permission(String code, String name) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException("Permission code must not be blank");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Permission name must not be blank");
        this.code = code.trim();
        this.name = name.trim();
    }

    public static Permission of(String code, String name) { return new Permission(code, name); }
    public static Permission create(String code, String name) { return new Permission(code, name); }
    public Long getId() { return super.getId(); }
    public String getCode() { return code; }
    public String getName() { return name; }
}
