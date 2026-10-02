package com.gymmind.workout.domain.model;
import com.gymmind.shared.persistence.BaseEntity;
import jakarta.persistence.*;

@Entity @Table(name="workout_plan_template_item")
public class WorkoutPlanTemplateItem extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="template_id",nullable=false) private WorkoutPlanTemplate template;
    @Column(name="exercise_id") private Long exerciseId;
    @Column(name="exercise_name",length=200) private String exerciseName;
    @Column(nullable=false,length=16) private String phase;
    @Column(nullable=false) private int sortOrder;
    @Column(nullable=false) private int sets;
    @Column(nullable=false) private int reps;
    @Column(nullable=false) private int restSeconds;
    @Column(nullable=false) private double weight;
    @Column(length=1000) private String notes;
    protected WorkoutPlanTemplateItem(){}
    public WorkoutPlanTemplateItem(WorkoutPlanTemplate t,String exerciseName,String phase,int sortOrder,int sets,int reps,int restSeconds,double weight,String notes){if(exerciseName==null||exerciseName.isBlank())throw new IllegalArgumentException("exerciseName is required");this.template=t;this.exerciseName=exerciseName.trim();this.phase=phase;this.sortOrder=sortOrder;this.sets=sets;this.reps=reps;this.restSeconds=restSeconds;this.weight=weight;this.notes=notes;}
    public WorkoutPlanTemplate getTemplate(){return template;} public Long getExerciseId(){return exerciseId;} public String getExerciseName(){return exerciseName;} public String getPhase(){return phase;} public int getSortOrder(){return sortOrder;} public int getSets(){return sets;} public int getReps(){return reps;} public int getRestSeconds(){return restSeconds;} public double getWeight(){return weight;} public String getNotes(){return notes;}
}
