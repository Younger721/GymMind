package com.gymmind.iam.domain.model;

import com.gymmind.tenancy.domain.model.Tenant;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/** A tenant-scoped override of a built-in role's permission set. */
@Entity
@Table(name = "sys_tenant_role_permission")
public class TenantRolePermission {
    @EmbeddedId
    private Key id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("tenantId")
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("permissionId")
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    protected TenantRolePermission() {
    }

    private TenantRolePermission(Tenant tenant, Role role, Permission permission) {
        if (tenant == null || role == null || permission == null) {
            throw new IllegalArgumentException("Tenant, role and permission are required");
        }
        this.tenant = tenant;
        this.role = role;
        this.permission = permission;
        this.id = new Key(tenant.getId(), role.getId(), permission.getId());
    }

    public static TenantRolePermission link(Tenant tenant, Role role, Permission permission) {
        return new TenantRolePermission(tenant, role, permission);
    }

    public Key getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Role getRole() {
        return role;
    }

    public Permission getPermission() {
        return permission;
    }

    @jakarta.persistence.Embeddable
    public static class Key implements java.io.Serializable {
        private Long tenantId;
        private Long roleId;
        private Long permissionId;

        protected Key() {
        }

        public Key(Long tenantId, Long roleId, Long permissionId) {
            this.tenantId = tenantId;
            this.roleId = roleId;
            this.permissionId = permissionId;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public Long getRoleId() {
            return roleId;
        }

        public Long getPermissionId() {
            return permissionId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key key)) return false;
            return java.util.Objects.equals(tenantId, key.tenantId)
                    && java.util.Objects.equals(roleId, key.roleId)
                    && java.util.Objects.equals(permissionId, key.permissionId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, roleId, permissionId);
        }
    }
}
