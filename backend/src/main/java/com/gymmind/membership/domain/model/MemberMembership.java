package com.gymmind.membership.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "member_membership")
public class MemberMembership extends TenantScopedEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "package_id", nullable = false)
    private Long packageId;

    @Column(name = "remaining_count")
    private Integer remainingCount;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MemberMembershipStatus status = MemberMembershipStatus.ACTIVE;

    /** 停卡区间开始日（审批通过后写入，解冻后清空） */
    @Column(name = "freeze_start")
    private LocalDate freezeStart;

    /** 停卡区间结束日（含当日） */
    @Column(name = "freeze_until")
    private LocalDate freezeUntil;

    protected MemberMembership() {
    }

    private MemberMembership(Long tenantId, Long memberId, Long packageId, Integer remainingCount,
                             Instant startsAt, Instant expiresAt) {
        super(tenantId);
        if (memberId == null || packageId == null || startsAt == null) {
            throw new IllegalArgumentException("Invalid membership");
        }
        this.memberId = memberId;
        this.packageId = packageId;
        this.remainingCount = remainingCount;
        this.startsAt = startsAt;
        this.expiresAt = expiresAt;
    }

    public static MemberMembership create(Long tenantId, Long memberId, Long packageId, Integer remainingCount,
                                          Instant startsAt, Instant expiresAt) {
        return new MemberMembership(tenantId, memberId, packageId, remainingCount, startsAt, expiresAt);
    }

    public void consume() {
        if (remainingCount == null || remainingCount <= 0) {
            throw new IllegalStateException("No remaining entitlement");
        }
        remainingCount--;
    }

    public void restore() {
        if (remainingCount != null) {
            remainingCount++;
        }
    }

    /** 审批通过停卡：顺延有效期并按当前日期决定是否立即停卡 */
    public void applyApprovedFreeze(LocalDate start, LocalDate end, LocalDate today) {
        if (start == null || end == null || end.isBefore(start)) {
            throw new IllegalArgumentException("Invalid freeze period");
        }
        if (status != MemberMembershipStatus.ACTIVE && status != MemberMembershipStatus.SUSPENDED) {
            throw new IllegalStateException("Membership cannot be frozen");
        }
        long freezeDays = ChronoUnit.DAYS.between(start, end) + 1;
        if (expiresAt != null && freezeDays > 0) {
            expiresAt = expiresAt.plus(freezeDays, ChronoUnit.DAYS);
        }
        freezeStart = start;
        freezeUntil = end;
        refreshFreezeStatus(today);
    }

    /** 按日历日刷新停卡/激活状态（定时任务或审批时调用） */
    public void refreshFreezeStatus(LocalDate today) {
        if (freezeStart == null || freezeUntil == null) {
            return;
        }
        if (today.isBefore(freezeStart)) {
            status = MemberMembershipStatus.ACTIVE;
            return;
        }
        if (!today.isAfter(freezeUntil)) {
            status = MemberMembershipStatus.SUSPENDED;
            return;
        }
        clearFreezePeriod();
    }

    public void clearFreezePeriod() {
        freezeStart = null;
        freezeUntil = null;
        if (status == MemberMembershipStatus.SUSPENDED) {
            status = MemberMembershipStatus.ACTIVE;
        }
    }

    public void transferTo(Long newMemberId) {
        if (newMemberId == null || newMemberId.equals(memberId)) {
            throw new IllegalArgumentException("Invalid transfer target");
        }
        if (status != MemberMembershipStatus.ACTIVE) {
            throw new IllegalStateException("Only active membership can be transferred");
        }
        memberId = newMemberId;
    }

    public boolean isUsableAt(Instant now) {
        if (status != MemberMembershipStatus.ACTIVE) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(now)) {
            return false;
        }
        if (remainingCount != null && remainingCount <= 0) {
            return false;
        }
        return true;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getPackageId() {
        return packageId;
    }

    public Integer getRemainingCount() {
        return remainingCount;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public MemberMembershipStatus getStatus() {
        return status;
    }

    public LocalDate getFreezeStart() {
        return freezeStart;
    }

    public LocalDate getFreezeUntil() {
        return freezeUntil;
    }
}
