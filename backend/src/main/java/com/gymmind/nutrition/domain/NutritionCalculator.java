package com.gymmind.nutrition.domain;
public interface NutritionCalculator { double calculateBmi(double weightKg, double heightCm); double calculateBmr(Sex sex, double weightKg, double heightCm, int age); double calculateTdee(double bmr, double activityFactor); MacroTargets calculateMacros(double calories, double proteinRatio, double carbohydrateRatio, double fatRatio); }
