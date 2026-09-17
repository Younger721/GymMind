package com.gymmind.ai.api;

import com.gymmind.analytics.application.WeeklyReport;
import com.gymmind.analytics.application.WeeklyReportService;
import com.gymmind.shared.api.ApiResponse;
import com.gymmind.shared.security.CurrentActorProvider;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/ai")
@SecurityRequirement(name = "bearerAuth")
public class AiWeeklyReportController {

    private final WeeklyReportService reportService;
    private final CurrentActorProvider actors;

    public AiWeeklyReportController(WeeklyReportService reportService, CurrentActorProvider actors) {
        this.reportService = reportService;
        this.actors = actors;
    }

    @GetMapping("/weekly-reports")
    public ApiResponse<WeeklyReport> weeklyReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return ApiResponse.success(reportService.report(actors.requireCurrent(), weekStart));
    }
}
