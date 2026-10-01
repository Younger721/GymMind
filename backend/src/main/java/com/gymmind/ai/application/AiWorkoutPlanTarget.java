package com.gymmind.ai.application;

/** A member an authenticated actor may target for plan generation. */
public record AiWorkoutPlanTarget(Long memberId, String name, String memberNumber) {}
