package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.report.WeeklyReport;
import com.gymmind.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final WeeklyReportService weeklyReportService;

    @GetMapping("/weekly")
    public ApiResponse<WeeklyReport> getWeeklyReport(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        WeeklyReport report = weeklyReportService.generateWeeklyReport(startDate, endDate);
        return ApiResponse.success(report);
    }
}
