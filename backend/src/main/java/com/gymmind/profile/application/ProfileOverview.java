package com.gymmind.profile.application;

import com.gymmind.iam.domain.model.UserStatus;
import com.gymmind.member.domain.model.MemberStatus;
import com.gymmind.membership.domain.model.MemberMembershipStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ProfileOverview(
        Long userId,
        String username,
        String email,
        String displayName,
        UserStatus status,
        LocalDate from,
        LocalDate to,
        long checkInCount,
        MemberInfo member,
        MembershipInfo membership) {

    public record MemberInfo(Long id, String fullName, String memberNumber,
                             MemberStatus status, String checkInToken) {}

    public record MembershipInfo(Long id, String packageName, String membershipType,
                                 Instant startsAt, Instant expiresAt, MemberMembershipStatus status) {}
}
