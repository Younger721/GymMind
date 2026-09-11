package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.dashboard.DashboardSummary;
import com.gymmind.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ApiResponse<DashboardSummary> getDashboardSummary() {
        DashboardSummary summary = dashboardService.getDashboardSummary();
        return ApiResponse.success(summary);
    }
}
