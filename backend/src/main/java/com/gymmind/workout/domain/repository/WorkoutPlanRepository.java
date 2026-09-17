package com.gymmind.workout.domain.repository;
import com.gymmind.workout.domain.model.WorkoutPlan;

import java.util.List;
import java.util.Optional;

public interface WorkoutPlanRepository {
    WorkoutPlan save(WorkoutPlan plan);
    Optional<WorkoutPlan> findByTenantIdAndId(Long tenantId, Long id);
    List<WorkoutPlan> findAllByTenantId(Long tenantId);
}
