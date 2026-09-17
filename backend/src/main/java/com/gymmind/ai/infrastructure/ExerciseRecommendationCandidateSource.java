package com.gymmind.ai.infrastructure;

import com.gymmind.ai.application.RecommendationCandidate;
import com.gymmind.ai.application.RecommendationCandidateSource;
import com.gymmind.exercise.domain.model.Exercise;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Primary
@Component
public class ExerciseRecommendationCandidateSource implements RecommendationCandidateSource {

    private final ExerciseRepository exercises;

    public ExerciseRecommendationCandidateSource(ExerciseRepository exercises) {
        this.exercises = exercises;
    }

    @Override
    public List<RecommendationCandidate> findByTenant(Long tenantId) {
        if (tenantId == null || tenantId <= 0) {
            return List.of();
        }
        return exercises.findAllByTenantId(tenantId).stream()
                .map(this::toCandidate)
                .toList();
    }

    private RecommendationCandidate toCandidate(Exercise exercise) {
        Set<String> tags = parseTags(exercise.getTags());
        Set<String> contraindications = tags.stream()
                .filter(tag -> tag.startsWith("avoid:"))
                .map(tag -> tag.substring("avoid:".length()))
                .collect(Collectors.toSet());
        return new RecommendationCandidate(
                exercise.getId(),
                exercise.getTenantId(),
                exercise.getName(),
                tags,
                contraindications);
    }

    private static Set<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.toSet());
    }
}
