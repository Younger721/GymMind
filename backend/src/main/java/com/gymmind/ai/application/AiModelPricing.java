package com.gymmind.ai.application;

import java.math.BigDecimal;

public record AiModelPricing(
        Long id,
        String provider,
        String model,
        BigDecimal inputPricePerMillion,
        BigDecimal outputPricePerMillion,
        String currency,
        boolean enabled) {
}
