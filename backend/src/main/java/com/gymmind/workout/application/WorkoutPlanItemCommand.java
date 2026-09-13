package com.gymmind.workout.application;
public record WorkoutPlanItemCommand(Long exerciseId, int dayOfWeek, int sets, int reps, int restSeconds, double weight, String notes) {}
