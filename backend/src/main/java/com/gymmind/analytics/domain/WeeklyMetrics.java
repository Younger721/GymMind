package com.gymmind.analytics.domain;
import java.math.BigDecimal; import java.time.LocalDate;
public record WeeklyMetrics(LocalDate weekStart, LocalDate weekEnd, DashboardMetrics metrics) {}
