package com.gymmind.service;

import com.gymmind.dto.nutrition.NutritionCalculation;
import com.gymmind.dto.nutrition.NutritionRecordRequest;
import com.gymmind.dto.nutrition.NutritionRecordResponse;
import com.gymmind.dto.nutrition.DailyNutritionSummary;
import com.gymmind.dto.profile.UserProfileResponse;
import com.gymmind.entity.NutritionRecord;
import com.gymmind.repository.NutritionRecordRepository;
import com.gymmind.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NutritionService {

    private final NutritionRecordRepository nutritionRecordRepository;
    private final UserProfileService userProfileService;

    public NutritionCalculation calculateNutrition() {
        UserProfileResponse profile = userProfileService.getProfile();

        if (profile.getHeight() == null || profile.getWeight() == null ||
            profile.getAge() == null || profile.getGender() == null) {
            throw new RuntimeException("Please complete your profile first");
        }

        // BMI
        double heightM = profile.getHeight() / 100.0;
        double bmi = profile.getWeight() / (heightM * heightM);

        // BMR (Mifflin-St Jeor Equation)
        double bmr;
        if ("MALE".equalsIgnoreCase(profile.getGender())) {
            bmr = (10 * profile.getWeight()) + (6.25 * profile.getHeight()) - (5 * profile.getAge()) + 5;
        } else {
            bmr = (10 * profile.getWeight()) + (6.25 * profile.getHeight()) - (5 * profile.getAge()) - 161;
        }

        // TDEE (Activity level multiplier, assuming moderate)
        double activityMultiplier = 1.55; // Moderate activity
        if (profile.getWeeklyWorkoutDays() != null) {
            if (profile.getWeeklyWorkoutDays() <= 1) activityMultiplier = 1.2;
            else if (profile.getWeeklyWorkoutDays() <= 3) activityMultiplier = 1.375;
            else if (profile.getWeeklyWorkoutDays() <= 5) activityMultiplier = 1.55;
            else activityMultiplier = 1.725;
        }
        double tdee = bmr * activityMultiplier;

        // Target calories based on goal
        double targetCalories = tdee;
        if (profile.getFitnessGoal() != null) {
            switch (profile.getFitnessGoal()) {
                case "WEIGHT_LOSS": targetCalories = tdee - 500; break;
                case "MUSCLE_GAIN": targetCalories = tdee + 300; break;
                case "MAINTAIN": targetCalories = tdee; break;
            }
        }

        // Macros
        double protein = profile.getWeight() * 2.0; // 2g per kg bodyweight
        double fats = (targetCalories * 0.25) / 9; // 25% from fats
        double carbs = (targetCalories - (protein * 4) - (fats * 9)) / 4;

        return NutritionCalculation.builder()
                .bmi(Math.round(bmi * 10) / 10.0)
                .bmr(Math.round(bmr))
                .tdee(Math.round(tdee))
                .targetCalories(Math.round(targetCalories))
                .targetProtein(Math.round(protein))
                .targetCarbs(Math.round(carbs))
                .targetFats(Math.round(fats))
                .build();
    }

    @Transactional
    public NutritionRecordResponse createRecord(NutritionRecordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        NutritionRecord record = NutritionRecord.builder()
                .userId(userId)
                .recordDate(request.getRecordDate())
                .mealType(request.getMealType())
                .foodName(request.getFoodName())
                .calories(request.getCalories())
                .protein(request.getProtein())
                .carbs(request.getCarbs())
                .fats(request.getFats())
                .servingSize(request.getServingSize())
                .notes(request.getNotes())
                .build();

        record = nutritionRecordRepository.save(record);
        log.info("Created nutrition record: id={}, userId={}", record.getId(), userId);

        return toResponse(record);
    }

    public DailyNutritionSummary getDailySummary(LocalDate date) {
        Long userId = SecurityUtils.getCurrentUserId();

        Double totalCalories = nutritionRecordRepository.calculateDailyCalories(userId, date);
        Double totalProtein = nutritionRecordRepository.calculateDailyProtein(userId, date);
        Double totalCarbs = nutritionRecordRepository.calculateDailyCarbs(userId, date);
        Double totalFats = nutritionRecordRepository.calculateDailyFats(userId, date);

        NutritionCalculation targets = calculateNutrition();

        return DailyNutritionSummary.builder()
                .date(date)
                .totalCalories(totalCalories != null ? totalCalories : 0.0)
                .totalProtein(totalProtein != null ? totalProtein : 0.0)
                .totalCarbs(totalCarbs != null ? totalCarbs : 0.0)
                .totalFats(totalFats != null ? totalFats : 0.0)
                .targetCalories(targets.getTargetCalories())
                .targetProtein(targets.getTargetProtein())
                .targetCarbs(targets.getTargetCarbs())
                .targetFats(targets.getTargetFats())
                .build();
    }

    public List<NutritionRecordResponse> getRecordsByDate(LocalDate date) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<NutritionRecord> records = nutritionRecordRepository.findByUserIdAndRecordDate(userId, date);
        return records.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private NutritionRecordResponse toResponse(NutritionRecord record) {
        return NutritionRecordResponse.builder()
                .id(record.getId())
                .recordDate(record.getRecordDate())
                .mealType(record.getMealType())
                .foodName(record.getFoodName())
                .calories(record.getCalories())
                .protein(record.getProtein())
                .carbs(record.getCarbs())
                .fats(record.getFats())
                .servingSize(record.getServingSize())
                .notes(record.getNotes())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
