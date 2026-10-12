package com.gymmind.analytics.domain;

import java.util.ArrayList;
import java.util.List;

/** 可解释的会员流失风险评分（规则引擎，不依赖大模型）。 */
public final class ChurnRiskCalculator {

    public ChurnRiskResult calculate(ChurnSignals input) {
        if (input == null) {
            throw new IllegalArgumentException("signals required");
        }
        int score = 0;
        List<String> reasons = new ArrayList<>();
        int days = input.daysSinceLastCheckIn();
        int recentCheckIns = input.checkInsLast30Days();
        int previousCheckIns = input.checkInsPrevious30Days();
        int remaining = input.membershipDaysRemaining();

        if (days >= 30) {
            score += 45;
            reasons.add("长期未到店");
        } else if (days >= 14) {
            score += 25;
            reasons.add("到店间隔偏长");
        }
        if (recentCheckIns < 2) {
            score += 15;
            reasons.add("近30天到店偏少");
        }
        if (previousCheckIns >= 4 && recentCheckIns * 2 < previousCheckIns) {
            score += 25;
            reasons.add("到店频次下降");
        }
        if (input.bookingsLast30Days() == 0 && recentCheckIns < 4) {
            score += 10;
            reasons.add("近30天无课程预约");
        }
        if (input.bookingsLast30Days() > 0 && input.courseParticipationsLast30Days() == 0) {
            score += 10;
            reasons.add("有预约但未完课");
        }
        if (input.membershipExpiring() && remaining <= 14) {
            score += 30;
            reasons.add("会籍即将到期");
        }
        score = Math.min(100, score);
        ChurnRiskLevel level = score >= 70 ? ChurnRiskLevel.HIGH : score >= 30 ? ChurnRiskLevel.MEDIUM : ChurnRiskLevel.LOW;
        return new ChurnRiskResult(score, level, List.copyOf(reasons));
    }
}
