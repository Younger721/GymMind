package com.gymmind.service;

import com.gymmind.dto.dashboard.DashboardSummary;
import com.gymmind.dto.nutrition.NutritionCalculation;
import com.gymmind.entity.*;
import com.gymmind.repository.*;
import com.gymmind.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final WorkoutRecordRepository workoutRecordRepository;
    private final NutritionRecordRepository nutritionRecordRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final UserProfileRepository userProfileRepository;
    private final NutritionService nutritionService;

    public DashboardSummary getDashboardSummary() {
        Long userId = SecurityUtils.getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6); // 最近7天
        LocalDate monthStart = today.minusDays(29); // 最近30天

        return DashboardSummary.builder()
                .todayOverview(getTodayOverview(userId, today))
                .weeklyStats(getWeeklyStats(userId, weekStart, today))
                .knowledgeStats(getKnowledgeStats(userId))
                .recentWorkouts(getRecentWorkouts(userId, weekStart, today))
                .weightTrend(getWeightTrend(userId, monthStart, today))
                .build();
    }

    private DashboardSummary.TodayOverview getTodayOverview(Long userId, LocalDate today) {
        // 今日训练统计
        List<WorkoutRecord> todayWorkouts = workoutRecordRepository
                .findByUserIdAndWorkoutDate(userId, today);

        int workoutCount = todayWorkouts.size();
        double totalVolume = todayWorkouts.stream()
                .mapToDouble(w -> w.getSets() * w.getReps() * (w.getWeight() != null ? w.getWeight() : 0.0))
                .sum();

        // 今日营养统计
        List<NutritionRecord> todayNutrition = nutritionRecordRepository
                .findByUserIdAndRecordDate(userId, today);

        double caloriesConsumed = todayNutrition.stream()
                .mapToDouble(NutritionRecord::getCalories)
                .sum();

        double proteinConsumed = todayNutrition.stream()
                .mapToDouble(NutritionRecord::getProtein)
                .sum();

        // 营养目标
        NutritionCalculation calculation = nutritionService.calculateNutrition();

        return DashboardSummary.TodayOverview.builder()
                .date(today)
                .workoutCount(workoutCount)
                .totalVolume(totalVolume)
                .caloriesConsumed(caloriesConsumed)
                .caloriesTarget(calculation.getTargetCalories())
                .proteinConsumed(proteinConsumed)
                .proteinTarget(calculation.getProteinGrams())
                .build();
    }

    private DashboardSummary.WeeklyStats getWeeklyStats(Long userId, LocalDate startDate, LocalDate endDate) {
        List<WorkoutRecord> weekWorkouts = workoutRecordRepository
                .findByUserIdAndWorkoutDateBetween(userId, startDate, endDate);

        // 训练天数（去重）
        int workoutDays = (int) weekWorkouts.stream()
                .map(WorkoutRecord::getWorkoutDate)
                .distinct()
                .count();

        // 总训练容量
        double totalVolume = weekWorkouts.stream()
                .mapToDouble(w -> w.getSets() * w.getReps() * (w.getWeight() != null ? w.getWeight() : 0.0))
                .sum();

        // 平均热量
        List<NutritionRecord> weekNutrition = nutritionRecordRepository
                .findByUserIdAndRecordDateBetween(userId, startDate, endDate);

        Map<LocalDate, Double> dailyCalories = weekNutrition.stream()
                .collect(Collectors.groupingBy(
                        NutritionRecord::getRecordDate,
                        Collectors.summingDouble(NutritionRecord::getCalories)
                ));

        double avgCalories = dailyCalories.isEmpty() ? 0.0 :
                dailyCalories.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        return DashboardSummary.WeeklyStats.builder()
                .workoutDays(workoutDays)
                .totalVolume(totalVolume)
                .avgCalories(avgCalories)
                .totalWorkouts(weekWorkouts.size())
                .build();
    }

    private DashboardSummary.KnowledgeStats getKnowledgeStats(Long userId) {
        long total = knowledgeDocumentRepository.countByUserId(userId);
        long success = knowledgeDocumentRepository.countByUserIdAndStatus(userId, KnowledgeDocument.ProcessingStatus.SUCCESS);
        long processing = knowledgeDocumentRepository.countByUserIdAndStatus(userId, KnowledgeDocument.ProcessingStatus.PROCESSING);
        long failed = knowledgeDocumentRepository.countByUserIdAndStatus(userId, KnowledgeDocument.ProcessingStatus.FAILED);

        return DashboardSummary.KnowledgeStats.builder()
                .totalDocuments(total)
                .successDocuments(success)
                .processingDocuments(processing)
                .failedDocuments(failed)
                .build();
    }

    private List<DashboardSummary.DailyWorkoutData> getRecentWorkouts(Long userId, LocalDate startDate, LocalDate endDate) {
        List<WorkoutRecord> workouts = workoutRecordRepository
                .findByUserIdAndWorkoutDateBetween(userId, startDate, endDate);

        Map<LocalDate, List<WorkoutRecord>> groupedByDate = workouts.stream()
                .collect(Collectors.groupingBy(WorkoutRecord::getWorkoutDate));

        return groupedByDate.entrySet().stream()
                .map(entry -> {
                    LocalDate date = entry.getKey();
                    List<WorkoutRecord> dayWorkouts = entry.getValue();

                    int count = dayWorkouts.size();
                    double volume = dayWorkouts.stream()
                            .mapToDouble(w -> w.getSets() * w.getReps() * (w.getWeight() != null ? w.getWeight() : 0.0))
                            .sum();

                    return DashboardSummary.DailyWorkoutData.builder()
                            .date(date)
                            .workoutCount(count)
                            .totalVolume(volume)
                            .build();
                })
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .collect(Collectors.toList());
    }

    private List<DashboardSummary.WeightTrendData> getWeightTrend(Long userId, LocalDate startDate, LocalDate endDate) {
        // 从用户档案历史获取体重数据（简化版：只返回当前体重）
        // 实际项目中应该有体重记录表来跟踪历史体重
        List<DashboardSummary.WeightTrendData> trend = new ArrayList<>();

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(null);

        if (profile != null && profile.getWeight() != null) {
            // 模拟最近30天的体重数据（实际应从体重记录表读取）
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                trend.add(DashboardSummary.WeightTrendData.builder()
                        .date(date)
                        .weight(profile.getWeight())
                        .build());
            }
        }

        return trend;
    }
}
