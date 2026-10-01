package com.gymmind.workout.application;
import java.time.LocalDate; import java.util.List;
public record TrainingOverviewView(String role, LocalDate since, LocalDate until, Summary summary, List<RecordView> records, List<MemberSummary> members) {
 public record Summary(long workoutCount,long activeMembers,long totalMinutes,double averageMinutes,int streakDays){}
 public record RecordView(Long id,Long memberId,LocalDate workoutDate,int duration,int calories,String notes,Long planId){}
 public record MemberSummary(Long memberId,String name,long workoutCount,int totalMinutes,LocalDate lastWorkout){}
}
