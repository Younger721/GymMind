package com.gymmind.analytics.domain;

/**
 * 会员活跃度与流失风险输入信号（签到、预约、课程参与、会籍）。
 */
public record ChurnSignals(
        int daysSinceLastCheckIn,
        int checkInsLast30Days,
        int checkInsPrevious30Days,
        int bookingsLast30Days,
        int courseParticipationsLast30Days,
        int membershipDaysRemaining,
        boolean membershipExpiring) {

    public ChurnSignals {
        daysSinceLastCheckIn = Math.max(0, daysSinceLastCheckIn);
        checkInsLast30Days = Math.max(0, checkInsLast30Days);
        checkInsPrevious30Days = Math.max(0, checkInsPrevious30Days);
        bookingsLast30Days = Math.max(0, bookingsLast30Days);
        courseParticipationsLast30Days = Math.max(0, courseParticipationsLast30Days);
        membershipDaysRemaining = Math.max(0, membershipDaysRemaining);
    }

    /** 兼容旧版 churn-risks API（字段语义为签到/训练间隔与频次）。 */
    public static ChurnSignals fromLegacyApi(
            int daysSinceLastWorkout,
            int workoutsLast30Days,
            int workoutsPrevious30Days,
            int membershipDaysRemaining,
            boolean membershipExpiring) {
        return new ChurnSignals(
                daysSinceLastWorkout,
                workoutsLast30Days,
                workoutsPrevious30Days,
                0,
                0,
                membershipDaysRemaining,
                membershipExpiring);
    }
}
