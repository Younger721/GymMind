package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.workout.WorkoutRecordRequest;
import com.gymmind.dto.workout.WorkoutRecordResponse;
import com.gymmind.dto.workout.WorkoutStatistics;
import com.gymmind.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/workout")
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutService workoutService;

    @PostMapping("/record")
    public ApiResponse<WorkoutRecordResponse> createRecord(@Valid @RequestBody WorkoutRecordRequest request) {
        WorkoutRecordResponse response = workoutService.createRecord(request);
        return ApiResponse.success("Workout recorded successfully", response);
    }

    @GetMapping("/records")
    public ApiResponse<List<WorkoutRecordResponse>> getRecords(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<WorkoutRecordResponse> records = workoutService.getRecordsByDate(date);
        return ApiResponse.success(records);
    }

    @GetMapping("/statistics")
    public ApiResponse<WorkoutStatistics> getStatistics(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        WorkoutStatistics statistics = workoutService.getStatistics(startDate, endDate);
        return ApiResponse.success(statistics);
    }
}
