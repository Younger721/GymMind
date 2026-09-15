package com.gymmind.ai.application;

import java.util.Set;

public record RecommendationCandidate(Long id, Long tenantId, String name, Set<String> tags,
                                      Set<String> contraindications) {
    public RecommendationCandidate {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        contraindications = contraindications == null ? Set.of() : Set.copyOf(contraindications);
    }
}
