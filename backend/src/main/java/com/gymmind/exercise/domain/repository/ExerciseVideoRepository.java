package com.gymmind.exercise.domain.repository;

import com.gymmind.exercise.domain.model.ExerciseVideo;

import java.util.Optional;
import java.util.List;

public interface ExerciseVideoRepository {
    ExerciseVideo save(ExerciseVideo video);
    Optional<ExerciseVideo> findByTenantIdAndId(Long tenantId, Long id);
    default List<ExerciseVideo> searchByTenantId(Long tenantId, String query) { return List.of(); }
    void delete(ExerciseVideo video);
}
