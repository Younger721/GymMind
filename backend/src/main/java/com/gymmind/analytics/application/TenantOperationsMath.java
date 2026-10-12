package com.gymmind.analytics.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TenantOperationsMath {

    private TenantOperationsMath() {
    }

    public static BigDecimal changeRate(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? BigDecimal.ZERO.setScale(1) : BigDecimal.valueOf(100).setScale(1);
        }
        return BigDecimal.valueOf(current - previous).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP);
    }
}
