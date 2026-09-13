package com.gymmind.workout.infrastructure.persistence;
import com.gymmind.workout.domain.model.WorkoutPlanItem;
import com.gymmind.workout.domain.repository.WorkoutPlanItemRepository;
import org.springframework.stereotype.Repository;
@Repository class JpaWorkoutPlanItemRepository implements WorkoutPlanItemRepository { private final SpringDataWorkoutPlanItemRepository delegate; JpaWorkoutPlanItemRepository(SpringDataWorkoutPlanItemRepository delegate){this.delegate=delegate;} public WorkoutPlanItem save(WorkoutPlanItem item){return delegate.save(item);} }
