package com.gymmind.membership.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Shared presentation rules for membership package labels and expiry days. */
public final class MembershipDisplay {
    private MembershipDisplay() {}

    public static String type(Integer validDays, String packageName) {
        if (validDays != null) {
            if (validDays == 7) return "周卡";
            if (validDays == 30) return "月卡";
            if (validDays == 365) return "年卡";
        }
        return packageName == null || packageName.isBlank() ? "其他套餐" : packageName;
    }

    public static long daysRemaining(Instant expiresAt, Clock clock) {
        if (expiresAt == null) return 0;
        Duration remaining = Duration.between(clock.instant(), expiresAt);
        if (remaining.isNegative() || remaining.isZero()) return 0;
        return (remaining.getSeconds() + 86_399L) / 86_400L;
    }
}
