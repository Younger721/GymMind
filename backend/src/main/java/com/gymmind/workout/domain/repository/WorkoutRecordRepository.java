package com.gymmind.workout.domain.repository;

import com.gymmind.workout.domain.model.WorkoutRecord;

import java.time.LocalDate;
import java.util.List;

public interface WorkoutRecordRepository {
    WorkoutRecord save(WorkoutRecord record);

    List<WorkoutRecord> findRecentCompleted(Long tenantId, Long memberId,
                                            LocalDate since, LocalDate until, int limit);
}
