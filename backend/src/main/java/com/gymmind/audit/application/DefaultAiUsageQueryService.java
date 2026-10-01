package com.gymmind.audit.application;

import com.gymmind.ai.application.AiUsageRecord;
import com.gymmind.ai.application.AiUsageRepository;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DefaultAiUsageQueryService implements AiUsageQueryService {
    private final AiUsageRepository repository;

    public DefaultAiUsageQueryService(AiUsageRepository repository) { this.repository = repository; }

    @Override
    public List<AiUsageRecord> list(CurrentActor actor) {
        if (actor == null || (!actor.isPlatformAdmin() && !actor.roles().contains(RoleCode.GYM_ADMIN))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long tenantId = ActorAccess.tenantId(actor);
        return tenantId == null ? repository.findAll() : repository.findAllByTenantId(tenantId);
    }
}
