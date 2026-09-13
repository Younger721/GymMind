package com.gymmind.member.application;

import com.gymmind.member.domain.model.MemberStatus;

public record MemberView(Long id, Long tenantId, Long userId, String memberNumber, String fullName,
                         String phone, MemberStatus status) {}
