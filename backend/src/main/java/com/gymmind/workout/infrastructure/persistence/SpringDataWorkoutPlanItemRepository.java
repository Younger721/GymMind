package com.gymmind.workout.infrastructure.persistence;
import com.gymmind.workout.domain.model.WorkoutPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
interface SpringDataWorkoutPlanItemRepository extends JpaRepository<WorkoutPlanItem, Long> { List<WorkoutPlanItem> findAllByPlanIdOrderBySortOrderAscIdAsc(Long planId); void deleteAllByPlanId(Long planId); }
