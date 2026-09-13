package com.gymmind.member.application;

public record CreateMemberCommand(Long tenantId, String memberNumber, String fullName, String phone, Long userId) {}
