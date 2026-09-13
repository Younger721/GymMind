package com.gymmind.workout.infrastructure.persistence;
import com.gymmind.workout.domain.model.WorkoutPlan;
import com.gymmind.workout.domain.repository.WorkoutPlanRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository class JpaWorkoutPlanRepository implements WorkoutPlanRepository { private final SpringDataWorkoutPlanRepository delegate; JpaWorkoutPlanRepository(SpringDataWorkoutPlanRepository delegate){this.delegate=delegate;} public WorkoutPlan save(WorkoutPlan p){return delegate.save(p);} public Optional<WorkoutPlan> findByTenantIdAndId(Long t,Long i){return delegate.findByTenantIdAndId(t,i);} }
