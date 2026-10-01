package com.gymmind.member.domain.model;

import com.gymmind.shared.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(name = "member_profile", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_tenant_number", columnNames = {"tenant_id", "member_number"}),
        @UniqueConstraint(name = "uk_member_tenant_phone", columnNames = {"tenant_id", "phone"})
})
public class Member extends AuditableEntity {
    /** Legacy column retained for migration compatibility; tenant membership now lives in member_tenant. */
    @Column(name = "tenant_id")
    private Long tenantId;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "member_number", length = 64)
    private String memberNumber;
    @Column(nullable = false, length = 128)
    private String fullName;
    @Column(length = 32)
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MemberStatus status = MemberStatus.ACTIVE;
    @Column(name = "check_in_token", nullable = false, unique = true, length = 64)
    private String checkInToken = UUID.randomUUID().toString().replace("-", "");

    protected Member() {}

    private Member(Long tenantId, Long userId, String memberNumber, String fullName, String phone) {
        this.tenantId = tenantId;
        this.userId = userId;
        this.memberNumber = required(memberNumber, "memberNumber");
        this.fullName = required(fullName, "fullName");
        this.phone = required(phone, "phone");
    }

    public static Member create(Long tenantId, Long userId, String memberNumber, String fullName, String phone) {
        return new Member(tenantId, userId, memberNumber, fullName, phone);
    }

    /** Creates a global member profile. Tenant-specific data belongs to MemberTenant. */
    public static Member create(Long userId, String fullName) {
        return new Member(null, userId, null, fullName, null);
    }

    public void update(String fullName, String phone) {
        this.fullName = required(fullName, "fullName");
        this.phone = required(phone, "phone");
    }

    public void suspend() { status = MemberStatus.SUSPENDED; }
    public void activate() { status = MemberStatus.ACTIVE; }
    public Long getUserId() { return userId; }
    public Long getTenantId() { return tenantId; }
    public String getMemberNumber() { return memberNumber; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public MemberStatus getStatus() { return status; }
    public String getCheckInToken() { return checkInToken; }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            if ("memberNumber".equals(name) || "phone".equals(name)) return null;
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
