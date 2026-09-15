package com.gymmind.ai.application;

public record MemberAnalysisInput(int daysSinceLastWorkout, int workoutsLast30Days,
                                  int workoutsPrevious30Days, int membershipDaysRemaining,
                                  boolean membershipExpiring) {}
