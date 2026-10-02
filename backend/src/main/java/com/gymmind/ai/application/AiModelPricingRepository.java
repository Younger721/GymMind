package com.gymmind.ai.application;

import java.util.List;
import java.util.Optional;

public interface AiModelPricingRepository {
    List<AiModelPricing> findAll();
    Optional<AiModelPricing> findByProviderAndModel(String provider, String model);
    AiModelPricing save(AiModelPricing pricing);
}
