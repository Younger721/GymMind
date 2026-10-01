package com.gymmind.membership.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MembershipDisplayTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-29T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void mapsStandardDurationsToCardTypes() {
        assertEquals("周卡", MembershipDisplay.type(7, "anything"));
        assertEquals("月卡", MembershipDisplay.type(30, "anything"));
        assertEquals("年卡", MembershipDisplay.type(365, "anything"));
    }

    @Test
    void usesPackageNameForCustomDuration() {
        assertEquals("季卡", MembershipDisplay.type(90, "季卡"));
        assertEquals("其他套餐", MembershipDisplay.type(90, null));
    }

    @Test
    void roundsRemainingDaysUpAndClampsExpired() {
        assertEquals(2, MembershipDisplay.daysRemaining(Instant.parse("2026-10-01T00:01:00Z"), CLOCK));
        assertEquals(0, MembershipDisplay.daysRemaining(Instant.parse("2026-09-29T11:59:59Z"), CLOCK));
    }
}
