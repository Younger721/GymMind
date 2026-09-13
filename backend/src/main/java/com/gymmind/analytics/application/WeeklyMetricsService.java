package com.gymmind.analytics.application;
import com.gymmind.analytics.domain.WeeklyMetrics; import com.gymmind.shared.security.CurrentActor; import java.time.LocalDate;
public interface WeeklyMetricsService { WeeklyMetrics weekly(CurrentActor actor, LocalDate weekStart); }
