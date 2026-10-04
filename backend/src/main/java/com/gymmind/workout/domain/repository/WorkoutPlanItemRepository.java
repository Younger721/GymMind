package com.gymmind.workout.domain.repository;
import com.gymmind.workout.domain.model.WorkoutPlanItem;
import java.util.List;
public interface WorkoutPlanItemRepository { WorkoutPlanItem save(WorkoutPlanItem item); List<WorkoutPlanItem> findAllByPlanId(Long planId); }
