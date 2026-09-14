package com.gymmind.analytics.application;
import com.gymmind.shared.security.CurrentActor;
import java.time.LocalDate;
public interface WeeklyReportService { WeeklyReport report(CurrentActor actor, LocalDate weekStart); }
