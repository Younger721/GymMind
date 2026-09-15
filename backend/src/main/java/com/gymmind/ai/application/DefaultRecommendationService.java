package com.gymmind.ai.application;

import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import com.gymmind.shared.security.CurrentActor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class DefaultRecommendationService implements RecommendationService {
    private final RecommendationCandidateSource source;

    public DefaultRecommendationService(RecommendationCandidateSource source) { this.source = source; }

    @Override
    public List<RecommendationCandidate> recommend(CurrentActor actor, String goal, Set<String> healthConstraints) {
        if (actor == null || actor.tenantId() == null || !actor.hasPermission("ai:recommend")
                || goal == null || goal.isBlank()) throw new BusinessException(ErrorCode.FORBIDDEN);
        Set<String> blocked = healthConstraints == null ? Set.of() : healthConstraints;
        return Optional.ofNullable(source.findByTenant(actor.tenantId())).orElseGet(List::of).stream()
                .filter(c -> c != null && Objects.equals(c.tenantId(), actor.tenantId()))
                .filter(c -> c.tags().contains(goal))
                .filter(c -> Collections.disjoint(c.contraindications(), blocked))
                .toList();
    }
}
