package com.gymmind.analytics.domain;

import java.util.ArrayList;
import java.util.List;

public final class ChurnRiskCalculator {
    public ChurnRiskResult calculate(ChurnSignals input) {
        if (input == null) throw new IllegalArgumentException("signals required");
        int days = Math.max(0, input.daysSinceLastWorkout());
        int recent = Math.max(0, input.workoutsLast30Days());
        int previous = Math.max(0, input.workoutsPrevious30Days());
        int remaining = Math.max(0, input.membershipDaysRemaining());
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (days >= 30) { score += 45; reasons.add("长期未训练"); }
        else if (days >= 14) { score += 25; reasons.add("训练间隔偏长"); }
        if (previous >= 3 && recent * 2 < previous) { score += 25; reasons.add("训练频率下降"); }
        if (input.membershipExpiring() && remaining <= 14) { score += 30; reasons.add("会员即将到期"); }
        score = Math.min(100, score);
        ChurnRiskLevel level = score >= 70 ? ChurnRiskLevel.HIGH : score >= 30 ? ChurnRiskLevel.MEDIUM : ChurnRiskLevel.LOW;
        return new ChurnRiskResult(score, level, List.copyOf(reasons));
    }
}
