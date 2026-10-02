package com.gymmind.ai.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AiUsagePricing {
    private static final BigDecimal MILLION = new BigDecimal("1000000");

    private AiUsagePricing() {
    }

    public static BigDecimal calculate(Long inputTokens, Long outputTokens,
                                       BigDecimal inputPricePerMillion,
                                       BigDecimal outputPricePerMillion) {
        if (inputTokens == null || outputTokens == null
                || inputPricePerMillion == null || outputPricePerMillion == null) {
            return null;
        }
        return BigDecimal.valueOf(inputTokens).multiply(inputPricePerMillion)
                .add(BigDecimal.valueOf(outputTokens).multiply(outputPricePerMillion))
                .divide(MILLION, 6, RoundingMode.HALF_UP);
    }
}
