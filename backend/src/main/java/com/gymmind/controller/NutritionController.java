package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.nutrition.DailyNutritionSummary;
import com.gymmind.dto.nutrition.NutritionCalculation;
import com.gymmind.dto.nutrition.NutritionRecordRequest;
import com.gymmind.dto.nutrition.NutritionRecordResponse;
import com.gymmind.service.NutritionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/nutrition")
@RequiredArgsConstructor
public class NutritionController {

    private final NutritionService nutritionService;

    @GetMapping("/calculate")
    public ApiResponse<NutritionCalculation> calculateNutrition() {
        NutritionCalculation calculation = nutritionService.calculateNutrition();
        return ApiResponse.success(calculation);
    }

    @PostMapping("/record")
    public ApiResponse<NutritionRecordResponse> createRecord(@Valid @RequestBody NutritionRecordRequest request) {
        NutritionRecordResponse response = nutritionService.createRecord(request);
        return ApiResponse.success("Nutrition recorded successfully", response);
    }

    @GetMapping("/records")
    public ApiResponse<List<NutritionRecordResponse>> getRecords(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<NutritionRecordResponse> records = nutritionService.getRecordsByDate(date);
        return ApiResponse.success(records);
    }

    @GetMapping("/summary")
    public ApiResponse<DailyNutritionSummary> getDailySummary(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyNutritionSummary summary = nutritionService.getDailySummary(date);
        return ApiResponse.success(summary);
    }
}
