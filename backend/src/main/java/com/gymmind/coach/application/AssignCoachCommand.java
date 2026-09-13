package com.gymmind.coach.application;
public record AssignCoachCommand(Long tenantId, Long coachId, Long memberId) {}
