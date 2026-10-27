package com.gymmind.workout.application;

import java.time.LocalDate;

/** 会员训练日历排期（含计划名称，供前端展示） */
public record MemberWorkoutScheduleView(
        Long id,
        Long memberPlanId,
        Long workoutPlanId,
        Long memberId,
        LocalDate scheduledDate,
        String planName,
        String goal) {
}
