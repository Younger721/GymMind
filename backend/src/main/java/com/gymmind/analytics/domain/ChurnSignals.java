package com.gymmind.analytics.domain;
public record ChurnSignals(int daysSinceLastWorkout, int workoutsLast30Days, int workoutsPrevious30Days, int membershipDaysRemaining, boolean membershipExpiring) { }
