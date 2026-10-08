package com.gymmind.workout.domain.model;

import com.gymmind.exercise.domain.model.Exercise;
import com.gymmind.shared.persistence.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "workout_plan_item")
public class WorkoutPlanItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_id", nullable = false)
    private WorkoutPlan plan;
    @Column(name = "exercise_id") private Long exerciseId;
    @Column(name = "exercise_name_snapshot", nullable = false, length = 200) private String exerciseNameSnapshot;
    @Column(name = "day_of_week", nullable = false) private int dayOfWeek;
    @Column(nullable = false, length = 16) private String phase;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private int sets;
    @Column(nullable = false) private int reps;
    @Column(nullable = false) private int restSeconds;
    @Column(nullable = false) private double weight;
    @Column(length = 1000) private String notes;
    protected WorkoutPlanItem() {}
    private WorkoutPlanItem(WorkoutPlan plan, Long exerciseId, String exerciseNameSnapshot, int dayOfWeek, int sets, int reps, int restSeconds,
                            double weight, String notes, String phase, int sortOrder) {
        if (plan == null || (exerciseId != null && exerciseId <= 0) || exerciseNameSnapshot == null || exerciseNameSnapshot.isBlank()
                || exerciseNameSnapshot.length() > 200 || dayOfWeek < 1 || dayOfWeek > 7 || sets <= 0 || reps <= 0 || restSeconds < 0 || weight < 0
                || !java.util.Set.of("WARMUP", "MAIN", "STRETCH").contains(phase) || sortOrder <= 0)
            throw new IllegalArgumentException("Invalid workout plan item");
        this.plan = plan; this.exerciseId = exerciseId; this.exerciseNameSnapshot = exerciseNameSnapshot.trim(); this.dayOfWeek = dayOfWeek; this.sets = sets; this.reps = reps;
        this.restSeconds = restSeconds; this.weight = weight; this.notes = notes == null ? "" : notes.trim();
        this.phase = phase; this.sortOrder = sortOrder;
    }
    public static WorkoutPlanItem create(WorkoutPlan plan, Long exerciseId, int dayOfWeek, int sets, int reps,
                                         int restSeconds, double weight, String notes) {
        return new WorkoutPlanItem(plan, exerciseId, "动作 #" + exerciseId, dayOfWeek, sets, reps, restSeconds, weight, notes, "MAIN", 1);
    }
    public static WorkoutPlanItem create(WorkoutPlan plan, Long exerciseId, String exerciseNameSnapshot, int dayOfWeek, int sets, int reps, int restSeconds, double weight, String notes) { return new WorkoutPlanItem(plan, exerciseId, exerciseNameSnapshot, dayOfWeek, sets, reps, restSeconds, weight, notes, "MAIN", 1); }
    public static WorkoutPlanItem create(WorkoutPlan plan, Long exerciseId, String exerciseNameSnapshot, int dayOfWeek, int sets, int reps, int restSeconds, double weight, String notes, String phase, int sortOrder) { return new WorkoutPlanItem(plan, exerciseId, exerciseNameSnapshot, dayOfWeek, sets, reps, restSeconds, weight, notes, phase, sortOrder); }
    public WorkoutPlan getPlan() { return plan; } public Long getExerciseId() { return exerciseId; } public String getExerciseNameSnapshot() { return exerciseNameSnapshot; } public int getDayOfWeek() { return dayOfWeek; }
    public int getSets() { return sets; } public int getReps() { return reps; } public int getRestSeconds() { return restSeconds; }
    public double getWeight() { return weight; } public String getNotes() { return notes; }
    public String getPhase() { return phase; } public int getSortOrder() { return sortOrder; }
}
