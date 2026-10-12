package com.gymmind.analytics.application;

import java.time.LocalDate;

/** 按日/周/月聚合的门店签到趋势点 */
public record TenantOperationsTrendPoint(
        String bucket,
        LocalDate day,
        long uniqueVisitors,
        long checkInPersonTimes) {
}
