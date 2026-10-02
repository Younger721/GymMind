package com.gymmind.ai.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SpringDataAiModelPricingRepository extends JpaRepository<AiModelPricingEntity, Long> {
    List<AiModelPricingEntity> findAllByOrderByProviderAscModelAsc();
    Optional<AiModelPricingEntity> findByProviderAndModel(String provider, String model);
}
