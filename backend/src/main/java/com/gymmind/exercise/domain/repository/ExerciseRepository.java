package com.gymmind.exercise.domain.repository;

import com.gymmind.exercise.domain.model.Exercise;

import java.util.Optional;

public interface ExerciseRepository {
    Exercise save(Exercise exercise);
    Optional<Exercise> findByTenantIdAndId(Long tenantId, Long id);
    boolean existsByTenantIdAndName(Long tenantId, String name);
}
