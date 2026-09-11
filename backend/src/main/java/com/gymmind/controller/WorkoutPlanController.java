package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.plan.GeneratePlanRequest;
import com.gymmind.dto.plan.WorkoutPlanResponse;
import com.gymmind.service.WorkoutPlanGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/workout-plans")
@RequiredArgsConstructor
public class WorkoutPlanController {

    private final WorkoutPlanGeneratorService planGeneratorService;

    @PostMapping("/generate")
    public ApiResponse<WorkoutPlanResponse> generatePlan(@Valid @RequestBody GeneratePlanRequest request) {
        log.info("Generating workout plan: goal={}, weeks={}", request.getGoal(), request.getDurationWeeks());
        WorkoutPlanResponse plan = planGeneratorService.generatePlan(request);
        return ApiResponse.success(plan);
    }

    @GetMapping
    public ApiResponse<List<WorkoutPlanResponse>> getUserPlans() {
        List<WorkoutPlanResponse> plans = planGeneratorService.getUserPlans();
        return ApiResponse.success(plans);
    }

    @GetMapping("/{planId}")
    public ApiResponse<WorkoutPlanResponse> getPlanById(@PathVariable Long planId) {
        WorkoutPlanResponse plan = planGeneratorService.getPlanById(planId);
        return ApiResponse.success(plan);
    }

    @PutMapping("/{planId}/status")
    public ApiResponse<Void> updatePlanStatus(
            @PathVariable Long planId,
            @RequestParam String status) {
        // TODO: Implement status update
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{planId}")
    public ApiResponse<Void> deletePlan(@PathVariable Long planId) {
        // TODO: Implement plan deletion
        return ApiResponse.success(null);
    }
}
