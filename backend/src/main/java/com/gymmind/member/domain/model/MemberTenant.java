package com.gymmind.member.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A member's membership in a specific gym tenant. */
@Entity
@Table(name = "member_tenant", uniqueConstraints = @UniqueConstraint(
        name = "uk_member_tenant_member_gym", columnNames = {"member_id", "tenant_id"}))
public class MemberTenant extends TenantScopedEntity {
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "member_number", length = 64)
    private String memberNumber;

    @Column(length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MemberStatus status = MemberStatus.ACTIVE;

    protected MemberTenant() {}

    private MemberTenant(Long memberId, Long tenantId, String memberNumber, String phone) {
        super(tenantId);
        if (memberId == null || memberId <= 0) throw new IllegalArgumentException("memberId must be positive");
        if (tenantId == null || tenantId <= 0) throw new IllegalArgumentException("tenantId must be positive");
        this.memberId = memberId;
        this.memberNumber = normalize(memberNumber);
        this.phone = normalize(phone);
    }

    /** Creates a bare tenant membership for callers that populate profile fields separately. */
    public static MemberTenant create(Long memberId, Long tenantId) {
        return new MemberTenant(memberId, tenantId, null, null);
    }

    public static MemberTenant create(Long memberId, Long tenantId, String memberNumber, String phone) {
        return new MemberTenant(memberId, tenantId, memberNumber, phone);
    }

    public void update(String memberNumber, String phone) {
        this.memberNumber = normalize(memberNumber);
        this.phone = normalize(phone);
    }

    public void suspend() { status = MemberStatus.SUSPENDED; }
    public void activate() { status = MemberStatus.ACTIVE; }
    public Long getMemberId() { return memberId; }
    public String getMemberNumber() { return memberNumber; }
    public String getPhone() { return phone; }
    public MemberStatus getStatus() { return status; }

    private static String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
