package com.gymmind.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyReport {

    private LocalDate startDate;
    private LocalDate endDate;

    // 训练总结
    private WorkoutSummary workoutSummary;

    // 营养总结
    private NutritionSummary nutritionSummary;

    // AI分析
    private String aiAnalysis;

    // 改进建议
    private List<String> suggestions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkoutSummary {
        private Integer totalDays;
        private Integer totalWorkouts;
        private Double totalVolume;
        private Double avgVolumePerDay;
        private String mostFrequentExercise;
        private List<PersonalRecord> personalRecords;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonalRecord {
        private String exerciseName;
        private Double weight;
        private Integer reps;
        private LocalDate achievedDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NutritionSummary {
        private Double avgCalories;
        private Double avgProtein;
        private Double avgCarbs;
        private Double avgFats;
        private Double calorieTarget;
        private Double proteinTarget;
        private Integer daysOnTarget;
        private Integer totalDays;
    }
}
