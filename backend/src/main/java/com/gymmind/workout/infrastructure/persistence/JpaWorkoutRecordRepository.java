package com.gymmind.workout.infrastructure.persistence;

import com.gymmind.workout.domain.model.WorkoutRecord;
import com.gymmind.workout.domain.model.WorkoutRecordStatus;
import com.gymmind.workout.domain.repository.WorkoutRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
class JpaWorkoutRecordRepository implements WorkoutRecordRepository {
    private final SpringDataWorkoutRecordRepository delegate;

    JpaWorkoutRecordRepository(SpringDataWorkoutRecordRepository d) {
        delegate = d;
    }

    @Override
    public WorkoutRecord save(WorkoutRecord record) {
        return delegate.save(record);
    }

    @Override
    public List<WorkoutRecord> findRecentCompleted(Long tenantId, Long memberId,
                                                   LocalDate since, LocalDate until, int limit) {
        if (tenantId == null || memberId == null || since == null || until == null || limit <= 0) {
            return List.of();
        }
        return delegate.findByTenantIdAndMemberIdAndStatusAndWorkoutDateBetweenOrderByWorkoutDateDescIdDesc(
                tenantId, memberId, WorkoutRecordStatus.COMPLETED, since, until,
                PageRequest.of(0, Math.min(limit, 30)));
    }
}
