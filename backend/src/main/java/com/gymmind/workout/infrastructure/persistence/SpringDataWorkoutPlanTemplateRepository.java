package com.gymmind.workout.infrastructure.persistence;
import com.gymmind.workout.domain.model.*; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SpringDataWorkoutPlanTemplateRepository extends JpaRepository<WorkoutPlanTemplate,Long>{ List<WorkoutPlanTemplate> findAllByTenantIdAndStatus(Long tenantId,WorkoutPlanTemplateStatus status); Optional<WorkoutPlanTemplate> findByTenantIdAndId(Long tenantId,Long id); }
