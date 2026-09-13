package com.gymmind.exercise.domain.repository;

import com.gymmind.exercise.domain.model.ExerciseVideo;

import java.util.Optional;

public interface ExerciseVideoRepository {
    ExerciseVideo save(ExerciseVideo video);
    Optional<ExerciseVideo> findByTenantIdAndId(Long tenantId, Long id);
    void delete(ExerciseVideo video);
}
