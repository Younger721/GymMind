package com.gymmind.workout.application;
public record WorkoutPlanItemCommand(Long exerciseId, String exerciseName, int dayOfWeek, int sets, int reps, int restSeconds,
                                     double weight, String notes, String phase, int sortOrder) {
    public WorkoutPlanItemCommand(Long exerciseId, int dayOfWeek, int sets, int reps, int restSeconds,
                                  double weight, String notes, String phase, int sortOrder) {
        this(exerciseId, null, dayOfWeek, sets, reps, restSeconds, weight, notes, phase, sortOrder);
    }
    public WorkoutPlanItemCommand(Long exerciseId, int dayOfWeek, int sets, int reps, int restSeconds,
                                  double weight, String notes) {
        this(exerciseId, null, dayOfWeek, sets, reps, restSeconds, weight, notes, "MAIN", 0);
    }
}
