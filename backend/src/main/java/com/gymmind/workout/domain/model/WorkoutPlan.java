package com.gymmind.workout.domain.model;

import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "workout_plan")
public class WorkoutPlan extends TenantScopedEntity {
    @Column(name = "member_id", nullable = false) private Long memberId;
    @Column(name = "coach_id") private Long coachId;
    @Column(nullable = false, length = 128) private String name;
    @Column(nullable = false, length = 128) private String goal;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private WorkoutPlanStatus status = WorkoutPlanStatus.DRAFT;
    @Column(nullable = false, length = 2000) private String description;
    protected WorkoutPlan() {}
    private WorkoutPlan(Long tenantId, Long memberId, Long coachId, String name, String goal, LocalDate startDate,
                        LocalDate endDate, String description) {
        super(tenantId);
        if (memberId == null || memberId <= 0) throw new IllegalArgumentException("memberId must be positive");
        if (name == null || name.isBlank() || goal == null || goal.isBlank() || description == null || description.isBlank())
            throw new IllegalArgumentException("Plan text fields must not be blank");
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) throw new IllegalArgumentException("Invalid plan dates");
        this.memberId = memberId; this.coachId = coachId; this.name = name.trim(); this.goal = goal.trim();
        this.startDate = startDate; this.endDate = endDate; this.description = description.trim();
    }
    public static WorkoutPlan create(Long tenantId, Long memberId, Long coachId, String name, String goal,
                                     LocalDate startDate, LocalDate endDate, String description) {
        return new WorkoutPlan(tenantId, memberId, coachId, name, goal, startDate, endDate, description);
    }
    public void publish() { if (status != WorkoutPlanStatus.DRAFT) throw new IllegalStateException("Only draft can publish"); status = WorkoutPlanStatus.PUBLISHED; }
    public Long getMemberId() { return memberId; } public Long getCoachId() { return coachId; } public String getName() { return name; }
    public String getGoal() { return goal; } public LocalDate getStartDate() { return startDate; } public LocalDate getEndDate() { return endDate; }
    public WorkoutPlanStatus getStatus() { return status; } public String getDescription() { return description; }
}
