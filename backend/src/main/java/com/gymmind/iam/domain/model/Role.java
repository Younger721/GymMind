package com.gymmind.iam.domain.model;

import com.gymmind.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "sys_role", uniqueConstraints = @UniqueConstraint(name = "uk_sys_role_code", columnNames = "code"))
public class Role extends AuditableEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RoleCode code;

    @Column(nullable = false, length = 128)
    private String name;

    protected Role() {}

    private Role(RoleCode code, String name) {
        if (code == null) throw new IllegalArgumentException("Role code must not be null");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Role name must not be blank");
        this.code = code;
        this.name = name.trim();
    }

    public static Role of(RoleCode code, String name) { return new Role(code, name); }
    public static Role create(RoleCode code, String name) { return new Role(code, name); }
    public Long getId() { return super.getId(); }
    public RoleCode getCode() { return code; }
    public String getName() { return name; }
}
