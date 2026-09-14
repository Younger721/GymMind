package com.gymmind.ai.application;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
@Service public class DefaultAiUsageService {
    private final AiUsageRepository repository;
    public DefaultAiUsageService(AiUsageRepository repository){this.repository=repository;}
    public AiUsageRecord record(CurrentActor actor, AiUsageCommand c){
        if(actor==null||actor.tenantId()==null) throw new IllegalArgumentException("tenant actor required");
        if(c==null||c.model()==null||c.model().isBlank()||c.inputTokens()<0||c.outputTokens()<0||c.latencyMs()<0) throw new IllegalArgumentException("invalid usage");
        return repository.save(new AiUsageRecord(actor.tenantId(),actor.userId(),c.model().trim(),c.inputTokens(),c.outputTokens(),c.latencyMs(),c.providerRequestId(),c.status(),null));
    }
}
