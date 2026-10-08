package com.gymmind.workout.domain.model;
import com.gymmind.shared.persistence.TenantScopedEntity;
import jakarta.persistence.*; import java.time.LocalDate;
@Entity @Table(name="member_workout_schedule")
public class MemberWorkoutSchedule extends TenantScopedEntity {
 @Column(name="member_plan_id") private Long memberPlanId; @Column(name="workout_plan_id") private Long workoutPlanId; @Column(name="member_id",nullable=false) private Long memberId; @Column(name="scheduled_date",nullable=false) private LocalDate scheduledDate;
 protected MemberWorkoutSchedule(){} public MemberWorkoutSchedule(Long tenantId,Long memberPlanId,Long memberId,LocalDate date){super(tenantId);this.memberPlanId=memberPlanId;this.memberId=memberId;this.scheduledDate=date;}
 public static MemberWorkoutSchedule forWorkoutPlan(Long tenantId,Long planId,Long memberId,LocalDate date){var schedule=new MemberWorkoutSchedule(tenantId,null,memberId,date);schedule.workoutPlanId=planId;return schedule;}
 public Long getMemberPlanId(){return memberPlanId;} public Long getWorkoutPlanId(){return workoutPlanId;} public Long getMemberId(){return memberId;} public LocalDate getScheduledDate(){return scheduledDate;}
}
