package com.gymmind.iam.domain.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_role_permission")
public class RolePermission {
    @EmbeddedId
    private Key id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    protected RolePermission() {}
    private RolePermission(Role role, Permission permission) {
        if (role == null || permission == null) throw new IllegalArgumentException("Role and permission are required");
        this.role = role;
        this.permission = permission;
        this.id = new Key(role.getId(), permission.getId());
    }
    public static RolePermission link(Role role, Permission permission) { return new RolePermission(role, permission); }
    public Key getId() { return id; }

    @jakarta.persistence.Embeddable
    public static class Key implements java.io.Serializable {
        private Long roleId;
        private Long permissionId;
        protected Key() {}
        public Key(Long roleId, Long permissionId) { this.roleId = roleId; this.permissionId = permissionId; }
        public Long getRoleId() { return roleId; }
        public Long getPermissionId() { return permissionId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Key k)) return false; return java.util.Objects.equals(roleId, k.roleId) && java.util.Objects.equals(permissionId, k.permissionId); }
        @Override public int hashCode() { return java.util.Objects.hash(roleId, permissionId); }
    }
}
