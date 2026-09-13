package com.gymmind.workout.infrastructure.persistence;
import com.gymmind.workout.domain.model.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
interface SpringDataWorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> { Optional<WorkoutPlan> findByTenantIdAndId(Long tenantId, Long id); }
