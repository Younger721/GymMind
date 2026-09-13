package com.gymmind.analytics.api;
import com.gymmind.analytics.application.*; import com.gymmind.analytics.domain.*; import com.gymmind.shared.api.ApiResponse; import com.gymmind.shared.security.*; import io.swagger.v3.oas.annotations.security.SecurityRequirement; import org.springframework.format.annotation.DateTimeFormat; import org.springframework.web.bind.annotation.*; import java.time.LocalDate;
@RestController @RequestMapping("/api/v1/analytics") @SecurityRequirement(name="bearerAuth")
public class AnalyticsController { private final DashboardQueryService dashboard; private final WeeklyMetricsService weekly; private final CurrentActorProvider actors; public AnalyticsController(DashboardQueryService d,WeeklyMetricsService w,CurrentActorProvider a){dashboard=d;weekly=w;actors=a;}
 @GetMapping("/dashboard") public ApiResponse<DashboardMetrics> dashboard(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to){return ApiResponse.success(dashboard.dashboard(actors.requireCurrent(),from,to));}
 @GetMapping("/weekly-metrics") public ApiResponse<WeeklyMetrics> weekly(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate weekStart){return ApiResponse.success(weekly.weekly(actors.requireCurrent(),weekStart));}
}
