package com.gymmind.analytics.application;
import com.gymmind.analytics.domain.DashboardMetrics; import com.gymmind.shared.security.CurrentActor; import java.time.LocalDate;
public interface DashboardQueryService { DashboardMetrics dashboard(CurrentActor actor, LocalDate from, LocalDate to); }
