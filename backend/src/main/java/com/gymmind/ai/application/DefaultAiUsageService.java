package com.gymmind.ai.application;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
@Service public class DefaultAiUsageService {
    private final AiUsageRepository repository;
    private final AiModelPricingService pricing;

    public DefaultAiUsageService(AiUsageRepository repository){this(repository, null);}

    @org.springframework.beans.factory.annotation.Autowired
    public DefaultAiUsageService(AiUsageRepository repository, AiModelPricingService pricing) {
        this.repository = repository;
        this.pricing = pricing;
    }
    public AiUsageRecord record(CurrentActor actor, AiUsageCommand c){
        boolean platformAdmin = actor != null && actor.isPlatformAdmin();
        Long tenantId = platformAdmin ? null : ActorAccess.tenantId(actor);
        if (!platformAdmin && tenantId == null) throw new IllegalArgumentException("tenant actor required");
        if (c == null || c.model() == null || c.model().isBlank()
                || (c.inputTokens() != null && c.inputTokens() < 0)
                || (c.outputTokens() != null && c.outputTokens() < 0)
                || (c.totalTokens() != null && c.totalTokens() < 0)
                || c.latencyMs() < 0) {
            throw new IllegalArgumentException("invalid usage");
        }
        BigDecimal cost = c.estimatedCost();
        String currency = c.currency();
        if (cost == null && pricing != null && c.inputTokens() != null && c.outputTokens() != null) {
            AiModelPricing modelPricing = pricing.find(c.provider(), c.model());
            if (modelPricing != null) {
                cost = AiUsagePricing.calculate(c.inputTokens().longValue(), c.outputTokens().longValue(),
                        modelPricing.inputPricePerMillion(), modelPricing.outputPricePerMillion());
                currency = modelPricing.currency();
            }
        }
        return repository.save(new AiUsageRecord(tenantId, actor.userId(), c.model().trim(), c.inputTokens(),
                c.outputTokens(), c.latencyMs(), c.providerRequestId(), c.status(), null, c.provider(),
                c.feature(), c.totalTokens(), c.usageSource(), cost, currency, null));
    }

    public AiUsageRecord record(CurrentActor actor, ChatModelCallResult result, String feature) {
        if (result == null) {
            return record(actor, new AiUsageCommand("unknown", (Integer) null, (Integer) null, null,
                    0, null, "ERROR", null, null, feature, "UNAVAILABLE", null, null));
        }
        ModelUsage usage = result.usage();
        String model = usage == null || usage.model() == null || usage.model().isBlank() ? "unknown" : usage.model();
        AiUsageCommand command = new AiUsageCommand(model,
                usage == null ? null : usage.inputTokens(),
                usage == null ? null : usage.outputTokens(),
                usage == null ? null : usage.totalTokens(),
                result.latencyMs(),
                usage == null ? null : usage.providerRequestId(),
                result.status(), null,
                usage == null ? null : usage.provider(), feature,
                usage != null && usage.available() ? "PROVIDER" : "UNAVAILABLE", null, null);
        return record(actor, command);
    }
}
