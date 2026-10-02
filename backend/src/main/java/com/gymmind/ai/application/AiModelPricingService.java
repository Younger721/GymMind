package com.gymmind.ai.application;

import com.gymmind.shared.security.CurrentActor;

import java.math.BigDecimal;
import java.util.List;

public interface AiModelPricingService {
    List<AiModelPricing> list(CurrentActor actor);
    AiModelPricing upsert(CurrentActor actor, PricingCommand command);
    AiModelPricing find(String provider, String model);

    record PricingCommand(String provider, String model, BigDecimal inputPricePerMillion,
                          BigDecimal outputPricePerMillion, String currency, boolean enabled) {
    }
}
