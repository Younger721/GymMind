package com.gymmind.dto.dashboard;

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
public class DashboardSummary {

    // 今日概览
    private TodayOverview todayOverview;

    // 本周统计
    private WeeklyStats weeklyStats;

    // 知识库状态
    private KnowledgeStats knowledgeStats;

    // 最近训练记录（最近7天）
    private List<DailyWorkoutData> recentWorkouts;

    // 体重趋势（最近30天）
    private List<WeightTrendData> weightTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TodayOverview {
        private LocalDate date;
        private Integer workoutCount;
        private Double totalVolume;
        private Double caloriesConsumed;
        private Double caloriesTarget;
        private Double proteinConsumed;
        private Double proteinTarget;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyStats {
        private Integer workoutDays;
        private Double totalVolume;
        private Double avgCalories;
        private Integer totalWorkouts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeStats {
        private Long totalDocuments;
        private Long successDocuments;
        private Long processingDocuments;
        private Long failedDocuments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyWorkoutData {
        private LocalDate date;
        private Integer workoutCount;
        private Double totalVolume;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeightTrendData {
        private LocalDate date;
        private Double weight;
    }
}
