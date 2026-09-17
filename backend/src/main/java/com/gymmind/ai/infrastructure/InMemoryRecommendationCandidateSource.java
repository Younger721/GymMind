package com.gymmind.ai.infrastructure;

import com.gymmind.ai.application.RecommendationCandidate;
import com.gymmind.ai.application.RecommendationCandidateSource;
import org.springframework.stereotype.Component;

import java.util.List;

/** Default empty source until the exercise catalog adapter is configured. */
public class InMemoryRecommendationCandidateSource implements RecommendationCandidateSource {
    @Override
    public List<RecommendationCandidate> findByTenant(Long tenantId) {
        return List.of();
    }
}
