package com.gymmind.repository;

import com.gymmind.entity.WorkoutRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WorkoutRecordRepository extends JpaRepository<WorkoutRecord, Long> {

    List<WorkoutRecord> findByUserIdAndWorkoutDate(Long userId, LocalDate date);

    List<WorkoutRecord> findByUserIdAndWorkoutDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(DISTINCT w.workoutDate) FROM WorkoutRecord w WHERE w.userId = ?1 AND w.workoutDate BETWEEN ?2 AND ?3")
    long countDistinctWorkoutDays(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(w.sets * w.reps * w.weight) FROM WorkoutRecord w WHERE w.userId = ?1 AND w.workoutDate BETWEEN ?2 AND ?3")
    Double calculateTotalVolume(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT MAX(w.weight) FROM WorkoutRecord w WHERE w.userId = ?1 AND w.exerciseName = ?2")
    Double findMaxWeight(Long userId, String exerciseName);
}
