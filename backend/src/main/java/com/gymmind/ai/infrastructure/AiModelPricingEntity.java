package com.gymmind.ai.infrastructure;

import com.gymmind.ai.application.AiModelPricing;
import com.gymmind.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(name = "ai_model_pricing",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_model_pricing_provider_model", columnNames = {"provider", "model"}))
class AiModelPricingEntity extends BaseEntity {
    @Column(nullable = false, length = 32)
    private String provider;
    @Column(nullable = false, length = 100)
    private String model;
    @Column(name = "input_price_per_million", nullable = false, precision = 18, scale = 8)
    private BigDecimal inputPricePerMillion;
    @Column(name = "output_price_per_million", nullable = false, precision = 18, scale = 8)
    private BigDecimal outputPricePerMillion;
    @Column(nullable = false, length = 8)
    private String currency;
    @Column(nullable = false)
    private boolean enabled;

    protected AiModelPricingEntity() {
    }

    static AiModelPricingEntity from(AiModelPricing value) {
        AiModelPricingEntity entity = new AiModelPricingEntity();
        entity.provider = value.provider();
        entity.model = value.model();
        entity.inputPricePerMillion = value.inputPricePerMillion();
        entity.outputPricePerMillion = value.outputPricePerMillion();
        entity.currency = value.currency();
        entity.enabled = value.enabled();
        return entity;
    }

    AiModelPricing toValue() {
        return new AiModelPricing(getId(), provider, model, inputPricePerMillion,
                outputPricePerMillion, currency, enabled);
    }

    void updateFrom(AiModelPricing value) {
        this.provider = value.provider();
        this.model = value.model();
        this.inputPricePerMillion = value.inputPricePerMillion();
        this.outputPricePerMillion = value.outputPricePerMillion();
        this.currency = value.currency();
        this.enabled = value.enabled();
    }
}
