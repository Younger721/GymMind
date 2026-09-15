package com.gymmind.exercise.infrastructure.persistence;

import com.gymmind.exercise.domain.model.ExerciseVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

interface SpringDataExerciseVideoRepository extends JpaRepository<ExerciseVideo, Long> {
    Optional<ExerciseVideo> findByTenantIdAndId(Long tenantId, Long id);
    List<ExerciseVideo> findTop20ByTenantIdAndTitleContainingIgnoreCaseOrderByIdDesc(Long tenantId, String title);
}
