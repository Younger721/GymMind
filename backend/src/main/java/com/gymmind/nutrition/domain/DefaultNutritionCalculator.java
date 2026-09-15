package com.gymmind.nutrition.domain;

import org.springframework.stereotype.Component;

@Component
public class DefaultNutritionCalculator implements NutritionCalculator {
    @Override public double calculateBmi(double weightKg, double heightCm) {
        if (weightKg <= 0 || heightCm <= 0) throw new IllegalArgumentException("Weight and height must be positive");
        double meters = heightCm / 100.0;
        return weightKg / (meters * meters);
    }
    @Override public double calculateBmr(Sex sex, double weightKg, double heightCm, int age) {
        if (sex == null || weightKg <= 0 || heightCm <= 0 || age <= 0) throw new IllegalArgumentException("Invalid physiology");
        double base = 10 * weightKg + 6.25 * heightCm - 5 * age;
        return base + (sex == Sex.MALE ? 5 : -161);
    }
    @Override public double calculateTdee(double bmr, double activityFactor) {
        if (bmr <= 0 || activityFactor <= 0) throw new IllegalArgumentException("BMR and activity factor must be positive");
        return bmr * activityFactor;
    }
    @Override public MacroTargets calculateMacros(double calories, double proteinRatio, double carbohydrateRatio, double fatRatio) {
        if (calories <= 0 || proteinRatio < 0 || carbohydrateRatio < 0 || fatRatio < 0
                || Math.abs(proteinRatio + carbohydrateRatio + fatRatio - 1.0) > 0.000001) {
            throw new IllegalArgumentException("Macro ratios must be non-negative and sum to one");
        }
        return new MacroTargets(calories * proteinRatio / 4.0, calories * carbohydrateRatio / 4.0,
                calories * fatRatio / 9.0);
    }
}
