package com.gymmind.workout.infrastructure.persistence;

import com.gymmind.workout.domain.model.WorkoutRecord;
import com.gymmind.workout.domain.model.WorkoutRecordStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

interface SpringDataWorkoutRecordRepository extends JpaRepository<WorkoutRecord, Long> {
    List<WorkoutRecord> findByTenantIdAndWorkoutDateBetweenOrderByWorkoutDateDescIdDesc(Long tenantId, LocalDate since, LocalDate until);
    List<WorkoutRecord> findByTenantIdAndMemberIdAndStatusAndWorkoutDateBetweenOrderByWorkoutDateDescIdDesc(
            Long tenantId, Long memberId, WorkoutRecordStatus status, LocalDate since, LocalDate until,
            Pageable pageable);
}
