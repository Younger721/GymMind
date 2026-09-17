package com.gymmind.exercise.infrastructure.persistence;

import com.gymmind.exercise.domain.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SpringDataExerciseRepository extends JpaRepository<Exercise, Long> {
    Optional<Exercise> findByTenantIdAndId(Long tenantId, Long id);
    List<Exercise> findAllByTenantId(Long tenantId);
    boolean existsByTenantIdAndName(Long tenantId, String name);
}
