package com.gymmind.analytics.application;
import java.time.LocalDate;
public record WeeklyReport(LocalDate weekStart, LocalDate weekEnd, String summary) {}
