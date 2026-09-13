package com.gymmind.coach.application;
public record CreateCoachCommand(Long tenantId, String coachNumber, String fullName, String phone, Long userId) {}
