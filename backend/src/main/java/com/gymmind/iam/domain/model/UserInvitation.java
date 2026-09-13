package com.gymmind.iam.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "sys_user_invitation",
        uniqueConstraints = @UniqueConstraint(name = "uk_sys_user_invitation_token_hash", columnNames = "token_hash"))
public class UserInvitation extends TenantScopedEntity {

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, length = 32)
    private RoleCode role;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InvitationStatus status;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected UserInvitation() {
    }

    private UserInvitation(Long tenantId, String email, RoleCode role, String tokenHash, Instant expiresAt) {
        super(tenantId);
        if (tenantId == null || tenantId <= 0) {
            throw new IllegalArgumentException("Tenant id must be positive");
        }
        this.email = normalizeEmail(email);
        if (role != RoleCode.COACH && role != RoleCode.MEMBER) {
            throw new IllegalArgumentException("Invitation role must be COACH or MEMBER");
        }
        this.role = role;
        this.tokenHash = requireHash(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.status = InvitationStatus.PENDING;
    }

    public static UserInvitation issue(Long tenantId, String email, RoleCode role,
                                       String tokenHash, Instant expiresAt) {
        return new UserInvitation(tenantId, email, role, tokenHash, expiresAt);
    }

    public boolean isAcceptable(Instant now) {
        return status == InvitationStatus.PENDING && expiresAt.isAfter(now);
    }

    public void accept(Instant now) {
        if (!isAcceptable(now)) {
            throw new IllegalStateException("Invitation is not active");
        }
        status = InvitationStatus.ACCEPTED;
        acceptedAt = Objects.requireNonNull(now, "now");
    }

    public void revoke() {
        if (status == InvitationStatus.PENDING) {
            status = InvitationStatus.REVOKED;
        }
    }

    public Long getTenantId() { return super.getTenantId(); }
    public String getEmail() { return email; }
    public RoleCode getRole() { return role; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public InvitationStatus getStatus() { return status; }
    public Instant getAcceptedAt() { return acceptedAt; }

    public static String sha256(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Invitation token must not be blank");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String normalizeEmail(String value) {
        return UserAccount.normalizeEmail(value);
    }

    private static String requireHash(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Invitation token hash must be SHA-256");
        }
        return value;
    }
}
