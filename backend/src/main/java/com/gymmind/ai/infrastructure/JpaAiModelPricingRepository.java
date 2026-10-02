package com.gymmind.ai.infrastructure;

import com.gymmind.ai.application.AiModelPricing;
import com.gymmind.ai.application.AiModelPricingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JpaAiModelPricingRepository implements AiModelPricingRepository {
    private final SpringDataAiModelPricingRepository delegate;

    public JpaAiModelPricingRepository(SpringDataAiModelPricingRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<AiModelPricing> findAll() {
        return delegate.findAllByOrderByProviderAscModelAsc().stream().map(AiModelPricingEntity::toValue).toList();
    }

    @Override
    public Optional<AiModelPricing> findByProviderAndModel(String provider, String model) {
        return delegate.findByProviderAndModel(provider, model).map(AiModelPricingEntity::toValue);
    }

    @Override
    public AiModelPricing save(AiModelPricing pricing) {
        AiModelPricingEntity entity = delegate.findByProviderAndModel(pricing.provider(), pricing.model())
                .orElseGet(AiModelPricingEntity::new);
        entity.updateFrom(pricing);
        return delegate.save(entity).toValue();
    }
}
