package com.gymmind.repository;

import com.gymmind.entity.WorkoutPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutPlanRepository extends JpaRepository<WorkoutPlan, Long> {

    List<WorkoutPlan> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<WorkoutPlan> findByIdAndUserId(Long id, Long userId);

    List<WorkoutPlan> findByUserIdAndStatus(Long userId, String status);
}
