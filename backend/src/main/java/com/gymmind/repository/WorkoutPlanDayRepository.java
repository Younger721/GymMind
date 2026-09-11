package com.gymmind.repository;

import com.gymmind.entity.WorkoutPlanDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutPlanDayRepository extends JpaRepository<WorkoutPlanDay, Long> {

    List<WorkoutPlanDay> findByPlanIdOrderByWeekNumberAscDayNumberAsc(Long planId);

    List<WorkoutPlanDay> findByPlanIdAndWeekNumber(Long planId, Integer weekNumber);
}
