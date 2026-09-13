package com.gymmind.iam.domain.model;

import com.gymmind.shared.persistence.AuditableEntity;
import com.gymmind.tenancy.domain.model.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.util.Locale;

@Entity
@Table(name = "sys_user", uniqueConstraints = @UniqueConstraint(name = "uk_sys_user_email", columnNames = "normalized_email"))
public class UserAccount extends AuditableEntity {

    @Column(name = "tenant_id")
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", insertable = false, updatable = false,
            foreignKey = @jakarta.persistence.ForeignKey(name = "fk_sys_user_tenant"))
    private Tenant tenant;

    @Column(name = "normalized_email", nullable = false, length = 320)
    private String normalizedEmail;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserStatus status;

    /**
     * Convenience state for domain operations. Persistent role membership is
     * represented exclusively by the UserRole join entity.
     */
    @Transient
    private RoleCode roleCode;

    @Column(name = "token_version", nullable = false)
    private long tokenVersion;

    protected UserAccount() {
    }

    private UserAccount(Long tenantId, String email, String passwordHash, String displayName, RoleCode roleCode) {
        if (roleCode == null) {
            throw new IllegalArgumentException("Role code must not be null");
        }
        if (roleCode == RoleCode.PLATFORM_ADMIN && tenantId != null) {
            throw new IllegalArgumentException("Platform admin must not have a tenant");
        }
        if (roleCode != RoleCode.PLATFORM_ADMIN && tenantId == null) {
            throw new IllegalArgumentException("Tenant user requires a tenant");
        }
        this.tenantId = tenantId;
        this.normalizedEmail = normalizeEmail(email);
        this.passwordHash = require(passwordHash, "Password hash");
        this.displayName = require(displayName, "Display name");
        this.roleCode = roleCode;
        this.status = UserStatus.ACTIVE;
        this.tokenVersion = 0L;
    }

    public static UserAccount platformAdmin(String email, String passwordHash, String displayName) {
        return new UserAccount(null, email, passwordHash, displayName, RoleCode.PLATFORM_ADMIN);
    }

    public static UserAccount tenantUser(Long tenantId, String email, String passwordHash, String displayName) {
        return new UserAccount(tenantId, email, passwordHash, displayName, RoleCode.MEMBER);
    }

    public static UserAccount tenantUser(Long tenantId, String email, String passwordHash, String displayName, RoleCode roleCode) {
        if (roleCode == RoleCode.PLATFORM_ADMIN) {
            throw new IllegalArgumentException("Platform admin is not a tenant user");
        }
        return new UserAccount(tenantId, email, passwordHash, displayName, roleCode);
    }

    public void activate() {
        if (status != UserStatus.ACTIVE) {
            status = UserStatus.ACTIVE;
            incrementTokenVersion();
        }
    }

    public void disable() {
        if (status != UserStatus.DISABLED) {
            status = UserStatus.DISABLED;
            incrementTokenVersion();
        }
    }

    public void resetPassword(String newPasswordHash) {
        passwordHash = require(newPasswordHash, "Password hash");
        incrementTokenVersion();
    }

    public void changeRole(RoleCode newRoleCode) {
        if (newRoleCode == null) {
            throw new IllegalArgumentException("Role code must not be null");
        }
        if (newRoleCode == roleCode) {
            return;
        }
        if (newRoleCode == RoleCode.PLATFORM_ADMIN && tenantId != null) {
            throw new IllegalArgumentException("Tenant user cannot become platform admin");
        }
        if (newRoleCode != RoleCode.PLATFORM_ADMIN && tenantId == null) {
            throw new IllegalArgumentException("Platform user cannot receive tenant role");
        }
        roleCode = newRoleCode;
        incrementTokenVersion();
    }

    public void incrementTokenVersion() {
        tokenVersion++;
    }

    public Long getTenantId() { return tenantId; }
    public String getNormalizedEmail() { return normalizedEmail; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public UserStatus getStatus() { return status; }
    public RoleCode getRoleCode() { return roleCode; }
    public long getTokenVersion() { return tokenVersion; }

    public static String normalizeEmail(String value) {
        return require(value, "Email").trim().toLowerCase(Locale.ROOT);
    }

    private static String require(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }
}
