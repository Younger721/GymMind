package com.gymmind.workout.infrastructure.persistence;
import com.gymmind.workout.domain.model.*; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface SpringDataWorkoutPlanTemplateItemRepository extends JpaRepository<WorkoutPlanTemplateItem,Long>{ List<WorkoutPlanTemplateItem> findAllByTemplateIdOrderBySortOrder(Long templateId); }
