package com.gymmind.membership.application;

import com.gymmind.membership.domain.model.MembershipChangeRequest;
import com.gymmind.membership.domain.model.MembershipChangeRequestStatus;
import com.gymmind.membership.domain.model.MembershipChangeType;

import java.time.Instant;
import java.time.LocalDate;

public record MembershipChangeRequestView(
        Long id,
        Long tenantId,
        MembershipChangeType type,
        MembershipChangeRequestStatus status,
        Long membershipId,
        Long memberId,
        Long targetMemberId,
        LocalDate freezeStart,
        LocalDate freezeEnd,
        String reason,
        Long applicantUserId,
        Long reviewerId,
        Instant reviewedAt,
        String reviewComment,
        Instant createdAt) {

    public static MembershipChangeRequestView from(MembershipChangeRequest request) {
        return new MembershipChangeRequestView(
                request.getId(),
                request.getTenantId(),
                request.getType(),
                request.getStatus(),
                request.getMembershipId(),
                request.getMemberId(),
                request.getTargetMemberId(),
                request.getFreezeStart(),
                request.getFreezeEnd(),
                request.getReason(),
                request.getApplicantUserId(),
                request.getReviewerId(),
                request.getReviewedAt(),
                request.getReviewComment(),
                request.getCreatedAt());
    }
}
