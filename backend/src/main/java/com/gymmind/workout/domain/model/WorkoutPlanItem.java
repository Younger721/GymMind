package com.gymmind.workout.domain.model;

import com.gymmind.exercise.domain.model.Exercise;
import com.gymmind.shared.persistence.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "workout_plan_item")
public class WorkoutPlanItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_id", nullable = false)
    private WorkoutPlan plan;
    @Column(name = "exercise_id", nullable = false) private Long exerciseId;
    @Column(name = "day_of_week", nullable = false) private int dayOfWeek;
    @Column(nullable = false) private int sets;
    @Column(nullable = false) private int reps;
    @Column(nullable = false) private int restSeconds;
    @Column(nullable = false) private double weight;
    @Column(length = 1000) private String notes;
    protected WorkoutPlanItem() {}
    private WorkoutPlanItem(WorkoutPlan plan, Long exerciseId, int dayOfWeek, int sets, int reps, int restSeconds,
                            double weight, String notes) {
        if (plan == null || exerciseId == null || exerciseId <= 0 || dayOfWeek < 1 || dayOfWeek > 7 || sets <= 0 || reps <= 0 || restSeconds < 0 || weight < 0)
            throw new IllegalArgumentException("Invalid workout plan item");
        this.plan = plan; this.exerciseId = exerciseId; this.dayOfWeek = dayOfWeek; this.sets = sets; this.reps = reps;
        this.restSeconds = restSeconds; this.weight = weight; this.notes = notes == null ? "" : notes.trim();
    }
    public static WorkoutPlanItem create(WorkoutPlan plan, Long exerciseId, int dayOfWeek, int sets, int reps,
                                         int restSeconds, double weight, String notes) {
        return new WorkoutPlanItem(plan, exerciseId, dayOfWeek, sets, reps, restSeconds, weight, notes);
    }
    public WorkoutPlan getPlan() { return plan; } public Long getExerciseId() { return exerciseId; } public int getDayOfWeek() { return dayOfWeek; }
    public int getSets() { return sets; } public int getReps() { return reps; } public int getRestSeconds() { return restSeconds; }
    public double getWeight() { return weight; } public String getNotes() { return notes; }
}
