package com.gymmind.membership.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class MemberMembershipFreezeTest {

    @Test
    void extendsExpiryAndSuspendsDuringFreezeWindow() {
        Instant start = Instant.parse("2026-10-01T00:00:00Z");
        MemberMembership membership = MemberMembership.create(
                11L, 41L, 8L, null, start, start.plus(30, ChronoUnit.DAYS));

        membership.applyApprovedFreeze(
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 11));

        assertThat(membership.getStatus()).isEqualTo(MemberMembershipStatus.SUSPENDED);
        assertThat(membership.getExpiresAt()).isAfter(start.plus(30, ChronoUnit.DAYS));
    }

    @Test
    void resumesAfterFreezeEnds() {
        MemberMembership membership = MemberMembership.create(
                11L, 41L, 8L, null, Instant.now(), Instant.now().plus(30, ChronoUnit.DAYS));
        membership.applyApprovedFreeze(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 10, 2));

        membership.refreshFreezeStatus(LocalDate.of(2026, 10, 4));

        assertThat(membership.getStatus()).isEqualTo(MemberMembershipStatus.ACTIVE);
        assertThat(membership.getFreezeUntil()).isNull();
    }
}
