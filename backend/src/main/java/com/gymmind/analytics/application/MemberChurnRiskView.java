package com.gymmind.analytics.application;
import com.gymmind.analytics.domain.ChurnRiskLevel;
import java.time.Instant;
import java.util.List;
public record MemberChurnRiskView(Long memberId, String memberName, Instant lastCheckInAt, int checkInsLast30Days, int bookingsLast30Days, int courseParticipationsLast30Days, int membershipDaysRemaining, ChurnRiskLevel level, int score, List<String> reasons) {}
