package com.gymmind.workout.application;
import com.gymmind.workout.domain.model.*; import java.time.LocalDate; import java.util.List;
public record WorkoutPlanView(Long id, Long tenantId, Long memberId, Long coachId, String name, String goal,
                              LocalDate startDate, LocalDate endDate, WorkoutPlanStatus status, String description, List<WorkoutPlanItemView> items) {
    public static WorkoutPlanView from(WorkoutPlan p, List<WorkoutPlanItemView> items){return new WorkoutPlanView(p.getId(),p.getTenantId(),p.getMemberId(),p.getCoachId(),p.getName(),p.getGoal(),p.getStartDate(),p.getEndDate(),p.getStatus(),p.getDescription(),items);}
}
