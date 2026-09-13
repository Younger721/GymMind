package com.gymmind.workout.infrastructure.persistence;
import com.gymmind.workout.domain.model.WorkoutPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
interface SpringDataWorkoutPlanItemRepository extends JpaRepository<WorkoutPlanItem, Long> {}
