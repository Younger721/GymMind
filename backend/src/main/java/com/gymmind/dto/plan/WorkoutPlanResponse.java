package com.gymmind.dto.plan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutPlanResponse {

    private Long id;

    private String planName;

    private String description;

    private String goal;

    private String difficulty;

    private Integer durationWeeks;

    private Integer workoutsPerWeek;

    private LocalDate startDate;

    private LocalDate endDate;

    private String status;

    private Boolean isAiGenerated;

    private LocalDateTime createdAt;

    private List<WeekPlan> weeks;

    private PlanStats stats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeekPlan {
        private Integer weekNumber;
        private String weekTitle;
        private List<DayPlan> days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPlan {
        private Integer dayNumber;
        private String dayName;
        private Boolean isRestDay;
        private Boolean isCompleted;
        private List<Exercise> exercises;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Exercise {
        private String name;
        private String category; // STRENGTH, CARDIO, FLEXIBILITY
        private String muscleGroup; // CHEST, BACK, LEGS, etc.
        private Integer sets;
        private Integer reps;
        private String restTime; // e.g., "90s", "2min"
        private String notes;
        private String videoUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanStats {
        private Integer totalWorkouts;
        private Integer completedWorkouts;
        private Integer remainingWorkouts;
        private Double completionRate;
        private Integer currentWeek;
    }
}
