package com.gymmind.audit.application;

import com.gymmind.ai.application.AiUsageRecord;
import com.gymmind.ai.application.AiUsageRepository;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DefaultAiUsageQueryService implements AiUsageQueryService {
    private final AiUsageRepository repository;

    public DefaultAiUsageQueryService(AiUsageRepository repository) { this.repository = repository; }

    @Override
    public List<AiUsageRecord> list(CurrentActor actor) {
        if (actor == null || actor.tenantId() == null || actor.isPlatformAdmin() || !actor.hasPermission("audit:read")) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return repository.findAllByTenantId(actor.tenantId());
    }
}
