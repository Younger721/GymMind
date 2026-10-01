package com.gymmind.iam.application;

import java.util.Map;

public record UserStatisticsView(
        long totalUsers,
        long memberCount,
        long coachCount,
        long gymAdminCount,
        long platformAdminCount,
        Map<String, Long> membershipTypeCounts) {
}
