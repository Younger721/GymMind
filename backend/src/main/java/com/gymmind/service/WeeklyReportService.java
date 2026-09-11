package com.gymmind.service;

import com.gymmind.dto.nutrition.NutritionCalculation;
import com.gymmind.dto.report.WeeklyReport;
import com.gymmind.entity.NutritionRecord;
import com.gymmind.entity.UserProfile;
import com.gymmind.entity.WorkoutRecord;
import com.gymmind.repository.NutritionRecordRepository;
import com.gymmind.repository.UserProfileRepository;
import com.gymmind.repository.WorkoutRecordRepository;
import com.gymmind.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final WorkoutRecordRepository workoutRecordRepository;
    private final NutritionRecordRepository nutritionRecordRepository;
    private final UserProfileRepository userProfileRepository;
    private final NutritionService nutritionService;
    private final ChatClient.Builder chatClientBuilder;

    public WeeklyReport generateWeeklyReport(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getCurrentUserId();

        WeeklyReport.WorkoutSummary workoutSummary = generateWorkoutSummary(userId, startDate, endDate);
        WeeklyReport.NutritionSummary nutritionSummary = generateNutritionSummary(userId, startDate, endDate);
        String aiAnalysis = generateAiAnalysis(userId, workoutSummary, nutritionSummary);
        List<String> suggestions = generateSuggestions(workoutSummary, nutritionSummary);

        return WeeklyReport.builder()
                .startDate(startDate)
                .endDate(endDate)
                .workoutSummary(workoutSummary)
                .nutritionSummary(nutritionSummary)
                .aiAnalysis(aiAnalysis)
                .suggestions(suggestions)
                .build();
    }

    private WeeklyReport.WorkoutSummary generateWorkoutSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        List<WorkoutRecord> records = workoutRecordRepository
                .findByUserIdAndWorkoutDateBetween(userId, startDate, endDate);

        if (records.isEmpty()) {
            return WeeklyReport.WorkoutSummary.builder()
                    .totalDays(0)
                    .totalWorkouts(0)
                    .totalVolume(0.0)
                    .avgVolumePerDay(0.0)
                    .mostFrequentExercise("N/A")
                    .personalRecords(new ArrayList<>())
                    .build();
        }

        // 训练天数
        int totalDays = (int) records.stream()
                .map(WorkoutRecord::getWorkoutDate)
                .distinct()
                .count();

        // 总容量
        double totalVolume = records.stream()
                .mapToDouble(w -> w.getSets() * w.getReps() * (w.getWeight() != null ? w.getWeight() : 0.0))
                .sum();

        // 平均每天容量
        double avgVolumePerDay = totalDays > 0 ? totalVolume / totalDays : 0.0;

        // 最常训练的动作
        String mostFrequentExercise = records.stream()
                .collect(Collectors.groupingBy(WorkoutRecord::getExerciseName, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        // 个人记录（本周最大重量）
        List<WeeklyReport.PersonalRecord> personalRecords = records.stream()
                .collect(Collectors.groupingBy(
                        WorkoutRecord::getExerciseName,
                        Collectors.maxBy(Comparator.comparing(w -> w.getWeight() != null ? w.getWeight() : 0.0))
                ))
                .entrySet().stream()
                .filter(e -> e.getValue().isPresent())
                .map(e -> {
                    WorkoutRecord record = e.getValue().get();
                    return WeeklyReport.PersonalRecord.builder()
                            .exerciseName(record.getExerciseName())
                            .weight(record.getWeight())
                            .reps(record.getReps())
                            .achievedDate(record.getWorkoutDate())
                            .build();
                })
                .sorted(Comparator.comparing(WeeklyReport.PersonalRecord::getWeight).reversed())
                .limit(5)
                .collect(Collectors.toList());

        return WeeklyReport.WorkoutSummary.builder()
                .totalDays(totalDays)
                .totalWorkouts(records.size())
                .totalVolume(totalVolume)
                .avgVolumePerDay(avgVolumePerDay)
                .mostFrequentExercise(mostFrequentExercise)
                .personalRecords(personalRecords)
                .build();
    }

    private WeeklyReport.NutritionSummary generateNutritionSummary(Long userId, LocalDate startDate, LocalDate endDate) {
        List<NutritionRecord> records = nutritionRecordRepository
                .findByUserIdAndRecordDateBetween(userId, startDate, endDate);

        // 获取营养目标
        NutritionCalculation calculation = nutritionService.calculateNutrition();

        if (records.isEmpty()) {
            return WeeklyReport.NutritionSummary.builder()
                    .avgCalories(0.0)
                    .avgProtein(0.0)
                    .avgCarbs(0.0)
                    .avgFats(0.0)
                    .calorieTarget(calculation.getTargetCalories())
                    .proteinTarget(calculation.getProteinGrams())
                    .daysOnTarget(0)
                    .totalDays(0)
                    .build();
        }

        // 按日期分组汇总
        Map<LocalDate, Double> dailyCalories = records.stream()
                .collect(Collectors.groupingBy(
                        NutritionRecord::getRecordDate,
                        Collectors.summingDouble(NutritionRecord::getCalories)
                ));

        Map<LocalDate, Double> dailyProtein = records.stream()
                .collect(Collectors.groupingBy(
                        NutritionRecord::getRecordDate,
                        Collectors.summingDouble(NutritionRecord::getProtein)
                ));

        Map<LocalDate, Double> dailyCarbs = records.stream()
                .collect(Collectors.groupingBy(
                        NutritionRecord::getRecordDate,
                        Collectors.summingDouble(NutritionRecord::getCarbs)
                ));

        Map<LocalDate, Double> dailyFats = records.stream()
                .collect(Collectors.groupingBy(
                        NutritionRecord::getRecordDate,
                        Collectors.summingDouble(NutritionRecord::getFats)
                ));

        // 平均值
        double avgCalories = dailyCalories.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double avgProtein = dailyProtein.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double avgCarbs = dailyCarbs.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double avgFats = dailyFats.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        // 达标天数（误差范围内±10%）
        double targetCalories = calculation.getTargetCalories();
        int daysOnTarget = (int) dailyCalories.values().stream()
                .filter(cal -> Math.abs(cal - targetCalories) <= targetCalories * 0.1)
                .count();

        return WeeklyReport.NutritionSummary.builder()
                .avgCalories(avgCalories)
                .avgProtein(avgProtein)
                .avgCarbs(avgCarbs)
                .avgFats(avgFats)
                .calorieTarget(targetCalories)
                .proteinTarget(calculation.getProteinGrams())
                .daysOnTarget(daysOnTarget)
                .totalDays(dailyCalories.size())
                .build();
    }

    private String generateAiAnalysis(Long userId, WeeklyReport.WorkoutSummary workout, WeeklyReport.NutritionSummary nutrition) {
        // 获取用户档案
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        String prompt = buildAnalysisPrompt(profile, workout, nutrition);

        try {
            ChatClient chatClient = chatClientBuilder.build();
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            return "AI分析暂时不可用，请稍后重试。";
        }
    }

    private String buildAnalysisPrompt(UserProfile profile, WeeklyReport.WorkoutSummary workout, WeeklyReport.NutritionSummary nutrition) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("作为一名专业的健身教练，请根据以下数据进行本周训练和营养分析：\n\n");

        // 用户信息
        if (profile != null) {
            prompt.append("用户信息：\n");
            prompt.append("- 健身目标：").append(profile.getFitnessGoal()).append("\n");
            prompt.append("- 经验水平：").append(profile.getExperienceLevel()).append("\n");
        }

        // 训练数据
        prompt.append("\n训练总结：\n");
        prompt.append("- 训练天数：").append(workout.getTotalDays()).append("天\n");
        prompt.append("- 总训练次数：").append(workout.getTotalWorkouts()).append("次\n");
        prompt.append("- 总训练容量：").append(String.format("%.0f", workout.getTotalVolume())).append(" kg\n");
        prompt.append("- 最常训练动作：").append(workout.getMostFrequentExercise()).append("\n");

        // 营养数据
        prompt.append("\n营养总结：\n");
        prompt.append("- 平均热量摄入：").append(String.format("%.0f", nutrition.getAvgCalories())).append(" kcal（目标：").append(String.format("%.0f", nutrition.getCalorieTarget())).append(" kcal）\n");
        prompt.append("- 平均蛋白质摄入：").append(String.format("%.0f", nutrition.getAvgProtein())).append("g（目标：").append(String.format("%.0f", nutrition.getProteinTarget())).append("g）\n");
        prompt.append("- 达标天数：").append(nutrition.getDaysOnTarget()).append("/").append(nutrition.getTotalDays()).append("天\n");

        prompt.append("\n请提供：\n");
        prompt.append("1. 本周训练和营养执行情况的总体评价（2-3句话）\n");
        prompt.append("2. 亮点和进步（1-2点）\n");
        prompt.append("3. 需要改进的地方（1-2点）\n");
        prompt.append("4. 下周的重点建议（1-2点）\n\n");
        prompt.append("请用简洁、鼓励的语气，字数控制在200字以内。");

        return prompt.toString();
    }

    private List<String> generateSuggestions(WeeklyReport.WorkoutSummary workout, WeeklyReport.NutritionSummary nutrition) {
        List<String> suggestions = new ArrayList<>();

        // 训练频率建议
        if (workout.getTotalDays() < 3) {
            suggestions.add("建议增加训练频率至每周3-5次，以获得更好的训练效果");
        } else if (workout.getTotalDays() > 6) {
            suggestions.add("训练频率较高，注意安排休息日以促进身体恢复");
        }

        // 训练容量建议
        if (workout.getTotalDays() > 0 && workout.getAvgVolumePerDay() < 1000) {
            suggestions.add("训练容量偏低，可以适当增加重量或训练组数");
        }

        // 营养建议
        double calorieGap = nutrition.getAvgCalories() - nutrition.getCalorieTarget();
        if (Math.abs(calorieGap) > nutrition.getCalorieTarget() * 0.2) {
            if (calorieGap > 0) {
                suggestions.add(String.format("平均热量摄入超出目标%.0f kcal，建议适当控制饮食", calorieGap));
            } else {
                suggestions.add(String.format("平均热量摄入低于目标%.0f kcal，建议增加营养摄入", -calorieGap));
            }
        }

        // 蛋白质建议
        if (nutrition.getAvgProtein() < nutrition.getProteinTarget() * 0.8) {
            suggestions.add("蛋白质摄入不足，建议增加优质蛋白质来源如鸡胸肉、鱼类、蛋白粉");
        }

        // 一致性建议
        if (nutrition.getTotalDays() > 0 && nutrition.getDaysOnTarget() < nutrition.getTotalDays() * 0.5) {
            suggestions.add("营养摄入波动较大，建议保持更稳定的饮食习惯");
        }

        return suggestions;
    }
}
