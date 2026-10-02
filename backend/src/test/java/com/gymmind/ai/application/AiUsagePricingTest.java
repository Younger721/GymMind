package com.gymmind.ai.application;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AiUsagePricingTest {
    @Test
    void calculatesCostFromPricesPerMillionTokens() {
        var cost = AiUsagePricing.calculate(
                1_200L,
                800L,
                new BigDecimal("2.50"),
                new BigDecimal("10.00"));

        assertThat(cost).isEqualByComparingTo("0.011000");
    }

    @Test
    void returnsNullWhenProviderUsageIsUnavailable() {
        assertThat(AiUsagePricing.calculate(null, 10L,
                new BigDecimal("2.50"), new BigDecimal("10.00"))).isNull();
    }
}
