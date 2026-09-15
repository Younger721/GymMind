package com.gymmind.ai.application;

import java.util.List;

@FunctionalInterface
public interface RecommendationCandidateSource {
    List<RecommendationCandidate> findByTenant(Long tenantId);
}
