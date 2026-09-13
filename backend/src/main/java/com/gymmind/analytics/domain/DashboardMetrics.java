package com.gymmind.analytics.domain;
import java.math.BigDecimal; import java.time.LocalDate;
public record DashboardMetrics(LocalDate from, LocalDate to, long memberCount, long activeMemberCount, BigDecimal activeRate, long bookingCount, BigDecimal bookingRate, long checkInCount, BigDecimal checkInRate, BigDecimal recordedRevenue, BigDecimal renewalRate) {}
