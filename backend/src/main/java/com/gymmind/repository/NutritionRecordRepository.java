package com.gymmind.repository;

import com.gymmind.entity.NutritionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface NutritionRecordRepository extends JpaRepository<NutritionRecord, Long> {

    List<NutritionRecord> findByUserIdAndRecordDate(Long userId, LocalDate date);

    List<NutritionRecord> findByUserIdAndRecordDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(n.calories) FROM NutritionRecord n WHERE n.userId = ?1 AND n.recordDate = ?2")
    Double calculateDailyCalories(Long userId, LocalDate date);

    @Query("SELECT SUM(n.protein) FROM NutritionRecord n WHERE n.userId = ?1 AND n.recordDate = ?2")
    Double calculateDailyProtein(Long userId, LocalDate date);

    @Query("SELECT SUM(n.carbs) FROM NutritionRecord n WHERE n.userId = ?1 AND n.recordDate = ?2")
    Double calculateDailyCarbs(Long userId, LocalDate date);

    @Query("SELECT SUM(n.fats) FROM NutritionRecord n WHERE n.userId = ?1 AND n.recordDate = ?2")
    Double calculateDailyFats(Long userId, LocalDate date);
}
