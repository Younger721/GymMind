package com.gymmind.workout.infrastructure.persistence;

import com.gymmind.workout.domain.model.WorkoutSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataWorkoutSetRepository extends JpaRepository<WorkoutSet, Long> {
    List<WorkoutSet> findAllByRecord_IdIn(List<Long> recordIds);
}
