package com.gymmind.workout.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "workout_plan_template")
public class WorkoutPlanTemplate extends TenantScopedEntity {
    @Column(nullable=false,length=128) private String name;
    @Column(nullable=false,length=64) private String category;
    @Column(nullable=false,length=128) private String goal;
    @Column(nullable=false,length=2000) private String description;
    @Column(name="coach_id") private Long coachId;
    @Column(nullable=false) private int versionNumber = 1;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private WorkoutPlanTemplateStatus status = WorkoutPlanTemplateStatus.DRAFT;
    protected WorkoutPlanTemplate() {}
    public WorkoutPlanTemplate(Long tenantId, Long coachId, String name, String category, String goal, String description) {
        super(tenantId); this.coachId=coachId; this.name=name; this.category=category; this.goal=goal; this.description=description;
    }
    public void publish(){ status=WorkoutPlanTemplateStatus.PUBLISHED; }
    public void archive(){ status=WorkoutPlanTemplateStatus.ARCHIVED; }
    public Long getCoachId(){return coachId;} public String getName(){return name;} public String getCategory(){return category;}
    public String getGoal(){return goal;} public String getDescription(){return description;} public int getVersionNumber(){return versionNumber;}
    public WorkoutPlanTemplateStatus getStatus(){return status;}
}
