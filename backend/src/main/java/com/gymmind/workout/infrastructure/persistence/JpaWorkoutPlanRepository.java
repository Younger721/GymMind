package com.gymmind.workout.infrastructure.persistence;

import com.gymmind.workout.domain.model.WorkoutPlan;
import com.gymmind.workout.domain.repository.WorkoutPlanRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class JpaWorkoutPlanRepository implements WorkoutPlanRepository {
    private final SpringDataWorkoutPlanRepository delegate;

    JpaWorkoutPlanRepository(SpringDataWorkoutPlanRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public WorkoutPlan save(WorkoutPlan plan) {
        return delegate.save(plan);
    }

    @Override
    public Optional<WorkoutPlan> findByTenantIdAndId(Long tenantId, Long id) {
        return delegate.findByTenantIdAndId(tenantId, id);
    }

    @Override
    public List<WorkoutPlan> findAllByTenantId(Long tenantId) {
        return delegate.findAllByTenantId(tenantId);
    }
}
