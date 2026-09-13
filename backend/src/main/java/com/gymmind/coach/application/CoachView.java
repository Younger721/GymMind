package com.gymmind.coach.application;

import com.gymmind.coach.domain.model.CoachStatus;
public record CoachView(Long id, Long tenantId, Long userId, String coachNumber, String fullName, String phone, CoachStatus status) {}
