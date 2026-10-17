package com.gymmind.membership.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "membership_change_request")
public class MembershipChangeRequest extends TenantScopedEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MembershipChangeType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MembershipChangeRequestStatus status = MembershipChangeRequestStatus.PENDING;

    @Column(name = "membership_id", nullable = false)
    private Long membershipId;

    @Column(name = "applicant_user_id", nullable = false)
    private Long applicantUserId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "target_member_id")
    private Long targetMemberId;

    @Column(name = "freeze_start")
    private LocalDate freezeStart;

    @Column(name = "freeze_end")
    private LocalDate freezeEnd;

    @Column(length = 500)
    private String reason;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    protected MembershipChangeRequest() {
    }

    private MembershipChangeRequest(Long tenantId, MembershipChangeType type, Long membershipId, Long memberId,
                                    Long applicantUserId, String reason) {
        super(tenantId);
        if (tenantId == null || type == null || membershipId == null || memberId == null || applicantUserId == null) {
            throw new IllegalArgumentException("Invalid membership change request");
        }
        this.type = type;
        this.membershipId = membershipId;
        this.memberId = memberId;
        this.applicantUserId = applicantUserId;
        this.reason = normalize(reason);
    }

    public static MembershipChangeRequest createFreeze(Long tenantId, Long membershipId, Long memberId,
                                                       Long applicantUserId, LocalDate freezeStart, LocalDate freezeEnd,
                                                       String reason) {
        MembershipChangeRequest request = new MembershipChangeRequest(
                tenantId, MembershipChangeType.FREEZE, membershipId, memberId, applicantUserId, reason);
        if (freezeStart == null || freezeEnd == null || freezeEnd.isBefore(freezeStart)) {
            throw new IllegalArgumentException("Invalid freeze period");
        }
        request.freezeStart = freezeStart;
        request.freezeEnd = freezeEnd;
        return request;
    }

    public static MembershipChangeRequest createTransfer(Long tenantId, Long membershipId, Long memberId,
                                                         Long applicantUserId, Long targetMemberId, String reason) {
        if (targetMemberId == null || targetMemberId.equals(memberId)) {
            throw new IllegalArgumentException("Invalid transfer target");
        }
        MembershipChangeRequest request = new MembershipChangeRequest(
                tenantId, MembershipChangeType.TRANSFER, membershipId, memberId, applicantUserId, reason);
        request.targetMemberId = targetMemberId;
        return request;
    }

    public void approve(Long reviewerId, String comment) {
        ensurePending();
        if (reviewerId == null) {
            throw new IllegalArgumentException("Reviewer is required");
        }
        if (reviewerId.equals(applicantUserId)) {
            throw new IllegalArgumentException("Applicant cannot approve own request");
        }
        this.reviewerId = reviewerId;
        this.reviewedAt = Instant.now();
        this.reviewComment = normalize(comment);
        this.status = MembershipChangeRequestStatus.APPROVED;
    }

    public void reject(Long reviewerId, String comment) {
        ensurePending();
        if (reviewerId == null) {
            throw new IllegalArgumentException("Reviewer is required");
        }
        if (reviewerId.equals(applicantUserId)) {
            throw new IllegalArgumentException("Applicant cannot reject own request");
        }
        this.reviewerId = reviewerId;
        this.reviewedAt = Instant.now();
        this.reviewComment = normalize(comment);
        this.status = MembershipChangeRequestStatus.REJECTED;
    }

    public void cancel() {
        ensurePending();
        this.status = MembershipChangeRequestStatus.CANCELLED;
    }

    private void ensurePending() {
        if (status != MembershipChangeRequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public MembershipChangeType getType() {
        return type;
    }

    public MembershipChangeRequestStatus getStatus() {
        return status;
    }

    public Long getMembershipId() {
        return membershipId;
    }

    public Long getApplicantUserId() {
        return applicantUserId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getTargetMemberId() {
        return targetMemberId;
    }

    public LocalDate getFreezeStart() {
        return freezeStart;
    }

    public LocalDate getFreezeEnd() {
        return freezeEnd;
    }

    public String getReason() {
        return reason;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewComment() {
        return reviewComment;
    }
}
