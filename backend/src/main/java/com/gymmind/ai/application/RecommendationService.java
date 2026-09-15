package com.gymmind.ai.application;

import com.gymmind.shared.security.CurrentActor;
import java.util.List;
import java.util.Set;

public interface RecommendationService {
    List<RecommendationCandidate> recommend(CurrentActor actor, String goal, Set<String> healthConstraints);
}
