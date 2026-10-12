package com.gymmind.analytics.application;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 门店运营统计 REST 汇总视图（签到人次口径：gym_check_in 记录数） */
public record TenantOperationsSummaryView(
        LocalDate from,
        LocalDate to,
        String periodLabel,
        long totalMembers,
        long activeMembers,
        long uniqueVisitors,
        long checkInPersonTimes,
        Long previousUniqueVisitors,
        Long previousCheckInPersonTimes,
        BigDecimal uniqueVisitorsChangeRate,
        BigDecimal checkInPersonTimesChangeRate) {
}
