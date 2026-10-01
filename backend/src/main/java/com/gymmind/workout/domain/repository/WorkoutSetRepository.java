package com.gymmind.workout.domain.repository;

import com.gymmind.workout.domain.model.WorkoutSet;

import java.util.List;

public interface WorkoutSetRepository {
    WorkoutSet save(WorkoutSet set);

    List<WorkoutSet> findAllByRecordIds(List<Long> recordIds);
}
