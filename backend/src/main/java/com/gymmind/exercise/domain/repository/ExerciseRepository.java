package com.gymmind.exercise.domain.repository;

import com.gymmind.exercise.domain.model.Exercise;

import java.util.List;
import java.util.Optional;

public interface ExerciseRepository {
    Exercise save(Exercise exercise);
    Optional<Exercise> findByTenantIdAndId(Long tenantId, Long id);
    List<Exercise> findAllByTenantId(Long tenantId);
    boolean existsByTenantIdAndName(Long tenantId, String name);
}
