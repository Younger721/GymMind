package com.gymmind.iam.domain.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "sys_user_role")
public class UserRole {
    @EmbeddedId
    private Key id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("roleId")
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    protected UserRole() {}
    private UserRole(UserAccount user, Role role) {
        if (user == null || role == null) throw new IllegalArgumentException("User and role are required");
        user.changeRole(role.getCode());
        this.user = user;
        this.role = role;
        this.id = new Key(user.getId(), role.getId());
    }
    public static UserRole assign(UserAccount user, Role role) { return new UserRole(user, role); }
    public Key getId() { return id; }

    @jakarta.persistence.Embeddable
    public static class Key implements java.io.Serializable {
        private Long userId;
        private Long roleId;
        protected Key() {}
        public Key(Long userId, Long roleId) { this.userId = userId; this.roleId = roleId; }
        public Long getUserId() { return userId; }
        public Long getRoleId() { return roleId; }
        @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof Key k)) return false; return java.util.Objects.equals(userId, k.userId) && java.util.Objects.equals(roleId, k.roleId); }
        @Override public int hashCode() { return java.util.Objects.hash(userId, roleId); }
    }
}
