package com.gymmind.ai.harness.plugins;

public record MemberFollowUpItem(Long memberId, String memberName, String reason, int daysSinceLastCheckIn) {}
