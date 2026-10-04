package com.gymmind.workout.application;
import com.gymmind.workout.domain.model.WorkoutPlanItem;
public record WorkoutPlanItemView(Long id, Long exerciseId, String exerciseName, int dayOfWeek, int sets, int reps, int restSeconds, double weight, String notes) {
    public static WorkoutPlanItemView from(WorkoutPlanItem item) { return new WorkoutPlanItemView(item.getId(), item.getExerciseId(), item.getExerciseNameSnapshot(), item.getDayOfWeek(), item.getSets(), item.getReps(), item.getRestSeconds(), item.getWeight(), item.getNotes()); }
}
