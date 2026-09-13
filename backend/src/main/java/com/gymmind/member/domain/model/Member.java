package com.gymmind.member.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "member_profile", uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_tenant_number", columnNames = {"tenant_id", "member_number"}),
        @UniqueConstraint(name = "uk_member_tenant_phone", columnNames = {"tenant_id", "phone"})
})
public class Member extends TenantScopedEntity {
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "member_number", nullable = false, length = 64)
    private String memberNumber;
    @Column(nullable = false, length = 128)
    private String fullName;
    @Column(nullable = false, length = 32)
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MemberStatus status = MemberStatus.ACTIVE;

    protected Member() {}

    private Member(Long tenantId, Long userId, String memberNumber, String fullName, String phone) {
        super(requireTenant(tenantId));
        this.userId = userId;
        this.memberNumber = required(memberNumber, "memberNumber");
        this.fullName = required(fullName, "fullName");
        this.phone = required(phone, "phone");
    }

    public static Member create(Long tenantId, Long userId, String memberNumber, String fullName, String phone) {
        return new Member(tenantId, userId, memberNumber, fullName, phone);
    }

    public void update(String fullName, String phone) {
        this.fullName = required(fullName, "fullName");
        this.phone = required(phone, "phone");
    }

    public void suspend() { status = MemberStatus.SUSPENDED; }
    public void activate() { status = MemberStatus.ACTIVE; }
    public Long getUserId() { return userId; }
    public String getMemberNumber() { return memberNumber; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public MemberStatus getStatus() { return status; }

    private static Long requireTenant(Long value) {
        if (value == null || value <= 0) throw new IllegalArgumentException("tenantId must be positive");
        return value;
    }
    private static String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
