package com.gymmind.exercise.infrastructure.persistence;

import com.gymmind.exercise.domain.model.ExerciseVideo;
import com.gymmind.exercise.domain.repository.ExerciseVideoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
class JpaExerciseVideoRepository implements ExerciseVideoRepository {
    private final SpringDataExerciseVideoRepository delegate;

    JpaExerciseVideoRepository(SpringDataExerciseVideoRepository delegate) { this.delegate = delegate; }
    @Override public ExerciseVideo save(ExerciseVideo video) { return delegate.save(video); }
    @Override public Optional<ExerciseVideo> findByTenantIdAndId(Long tenantId, Long id) {
        return delegate.findByTenantIdAndId(tenantId, id);
    }
    @Override public List<ExerciseVideo> searchByTenantId(Long tenantId, String query) {
        return delegate.findTop20ByTenantIdAndTitleContainingIgnoreCaseOrderByIdDesc(tenantId, query);
    }
    @Override public void delete(ExerciseVideo video) { delegate.delete(video); }
}
