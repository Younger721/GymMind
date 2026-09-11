package com.gymmind.service;

import com.gymmind.dto.workout.WorkoutRecordRequest;
import com.gymmind.dto.workout.WorkoutRecordResponse;
import com.gymmind.dto.workout.WorkoutStatistics;
import com.gymmind.entity.WorkoutRecord;
import com.gymmind.repository.WorkoutRecordRepository;
import com.gymmind.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutRecordRepository workoutRecordRepository;

    @Transactional
    public WorkoutRecordResponse createRecord(WorkoutRecordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        WorkoutRecord record = WorkoutRecord.builder()
                .userId(userId)
                .workoutDate(request.getWorkoutDate())
                .exerciseName(request.getExerciseName())
                .muscleGroup(request.getMuscleGroup())
                .sets(request.getSets())
                .reps(request.getReps())
                .weight(request.getWeight())
                .duration(request.getDuration())
                .rpe(request.getRpe())
                .notes(request.getNotes())
                .build();

        record = workoutRecordRepository.save(record);
        log.info("Created workout record: id={}, userId={}", record.getId(), userId);

        return toResponse(record);
    }

    public List<WorkoutRecordResponse> getRecordsByDate(LocalDate date) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<WorkoutRecord> records = workoutRecordRepository.findByUserIdAndWorkoutDate(userId, date);
        return records.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public WorkoutStatistics getStatistics(LocalDate startDate, LocalDate endDate) {
        Long userId = SecurityUtils.getCurrentUserId();

        long workoutDays = workoutRecordRepository.countDistinctWorkoutDays(userId, startDate, endDate);
        Double totalVolume = workoutRecordRepository.calculateTotalVolume(userId, startDate, endDate);

        return WorkoutStatistics.builder()
                .workoutDays((int) workoutDays)
                .totalVolume(totalVolume != null ? totalVolume : 0.0)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private WorkoutRecordResponse toResponse(WorkoutRecord record) {
        return WorkoutRecordResponse.builder()
                .id(record.getId())
                .workoutDate(record.getWorkoutDate())
                .exerciseName(record.getExerciseName())
                .muscleGroup(record.getMuscleGroup())
                .sets(record.getSets())
                .reps(record.getReps())
                .weight(record.getWeight())
                .duration(record.getDuration())
                .rpe(record.getRpe())
                .notes(record.getNotes())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
