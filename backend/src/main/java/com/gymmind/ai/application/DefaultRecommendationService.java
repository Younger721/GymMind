package com.gymmind.ai.application;

import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import com.gymmind.shared.security.ActorAccess;
import com.gymmind.tenancy.application.TenantQuotaService;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DefaultRecommendationService implements RecommendationService {
    private final RecommendationCandidateSource source;
    private final TenantQuotaService quotas;

    public DefaultRecommendationService(RecommendationCandidateSource source, TenantQuotaService quotas) { this.source = source; this.quotas = quotas; }

    @Override
    public List<RecommendationCandidate> recommend(CurrentActor actor, String goal, Set<String> healthConstraints) {
        Long tenantId = ActorAccess.tenantId(actor);
        if (tenantId == null || !actor.hasPermission("ai:recommend")
                || goal == null || goal.isBlank()) throw new BusinessException(ErrorCode.FORBIDDEN);
        quotas.ensureAiModuleEnabled(tenantId);
        Set<String> blocked = healthConstraints == null ? Set.of() : healthConstraints;
        return Optional.ofNullable(source.findByTenant(tenantId)).orElseGet(List::of).stream()
                .filter(c -> c != null && Objects.equals(c.tenantId(), tenantId))
                .filter(c -> c.tags().contains(goal))
                .filter(c -> Collections.disjoint(c.contraindications(), blocked))
                .toList();
    }
}
