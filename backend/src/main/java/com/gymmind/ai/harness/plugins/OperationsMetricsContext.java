package com.gymmind.ai.harness.plugins;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OperationsMetricsContext(
        LocalDate from,
        LocalDate to,
        String periodLabel,
        long uniqueVisitors,
        long checkInCount,
        long activeMembers,
        long previousUniqueVisitors,
        long previousCheckInCount,
        BigDecimal visitorChangeRate,
        long bookings,
        long completedBookings,
        BigDecimal attendanceRate,
        long expiringWithin7Days,
        long expiringWithin30Days,
        long inactiveMembers,
        long workoutMembers,
        long workoutCount,
        List<MemberFollowUpItem> followUps) {
    public OperationsMetricsContext {
        followUps = followUps == null ? List.of() : List.copyOf(followUps);
    }
}
