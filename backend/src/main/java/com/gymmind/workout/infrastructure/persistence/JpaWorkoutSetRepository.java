package com.gymmind.workout.infrastructure.persistence;

import com.gymmind.workout.domain.model.WorkoutSet;
import com.gymmind.workout.domain.repository.WorkoutSetRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class JpaWorkoutSetRepository implements WorkoutSetRepository {
    private final SpringDataWorkoutSetRepository delegate;

    JpaWorkoutSetRepository(SpringDataWorkoutSetRepository d) {
        delegate = d;
    }

    @Override
    public WorkoutSet save(WorkoutSet set) {
        return delegate.save(set);
    }

    @Override
    public List<WorkoutSet> findAllByRecordIds(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return List.of();
        }
        return delegate.findAllByRecord_IdIn(recordIds);
    }
}
