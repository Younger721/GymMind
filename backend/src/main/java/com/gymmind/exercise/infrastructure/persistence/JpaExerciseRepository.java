package com.gymmind.exercise.infrastructure.persistence;

import com.gymmind.exercise.domain.model.Exercise;
import com.gymmind.exercise.domain.repository.ExerciseRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class JpaExerciseRepository implements ExerciseRepository {
    private final SpringDataExerciseRepository delegate;

    JpaExerciseRepository(SpringDataExerciseRepository delegate) { this.delegate = delegate; }

    @Override public Exercise save(Exercise exercise) { return delegate.save(exercise); }
    @Override public Optional<Exercise> findByTenantIdAndId(Long tenantId, Long id) {
        return delegate.findByTenantIdAndId(tenantId, id);
    }
    @Override public List<Exercise> findAllByTenantId(Long tenantId) {
        return delegate.findAllByTenantId(tenantId);
    }
    @Override public boolean existsByTenantIdAndName(Long tenantId, String name) {
        return delegate.existsByTenantIdAndName(tenantId, name);
    }
}
