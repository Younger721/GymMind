package com.gymmind.membership.application;

import com.gymmind.membership.domain.model.MembershipChangeType;

import java.time.LocalDate;

public record CreateMembershipChangeCommand(
        MembershipChangeType type,
        Long membershipId,
        LocalDate freezeStart,
        LocalDate freezeEnd,
        Long targetMemberId,
        String reason) {
}
