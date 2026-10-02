package com.gymmind.ai.application;

import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class DefaultAiModelPricingService implements AiModelPricingService {
    private final AiModelPricingRepository repository;

    public DefaultAiModelPricingService(AiModelPricingRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AiModelPricing> list(CurrentActor actor) {
        ActorAccess.requirePlatformAdmin(actor, "platform:ai-pricing:read");
        return repository.findAll();
    }

    @Override
    public AiModelPricing upsert(CurrentActor actor, PricingCommand command) {
        ActorAccess.requirePlatformAdmin(actor, "platform:ai-pricing:write");
        if (command == null || blank(command.provider()) || blank(command.model())
                || command.inputPricePerMillion() == null || command.outputPricePerMillion() == null
                || command.inputPricePerMillion().compareTo(BigDecimal.ZERO) < 0
                || command.outputPricePerMillion().compareTo(BigDecimal.ZERO) < 0
                || blank(command.currency())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        AiModelPricing value = new AiModelPricing(null, command.provider().trim(), command.model().trim(),
                command.inputPricePerMillion(), command.outputPricePerMillion(), command.currency().trim(),
                command.enabled());
        return repository.save(value);
    }

    @Override
    public AiModelPricing find(String provider, String model) {
        if (blank(provider) || blank(model)) return null;
        return repository.findByProviderAndModel(provider, model).filter(AiModelPricing::enabled).orElse(null);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
