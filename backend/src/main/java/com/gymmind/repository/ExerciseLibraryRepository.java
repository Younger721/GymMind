package com.gymmind.repository;

import com.gymmind.entity.ExerciseLibrary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 动作库数据访问层
 */
@Repository
public interface ExerciseLibraryRepository extends JpaRepository<ExerciseLibrary, Long> {

    /**
     * 根据类别查询动作（分页）
     */
    Page<ExerciseLibrary> findByCategoryAndStatus(String category, String status, Pageable pageable);

    /**
     * 根据难度查询动作（分页）
     */
    Page<ExerciseLibrary> findByDifficultyAndStatus(String difficulty, String status, Pageable pageable);

    /**
     * 根据器械查询动作（分页）
     */
    Page<ExerciseLibrary> findByEquipmentAndStatus(String equipment, String status, Pageable pageable);

    /**
     * 查询所有激活状态的动作（分页）
     */
    Page<ExerciseLibrary> findByStatus(String status, Pageable pageable);

    /**
     * 根据名称模糊查询动作
     */
    @Query("SELECT e FROM ExerciseLibrary e WHERE e.status = :status AND (e.name LIKE %:keyword% OR e.nameEn LIKE %:keyword% OR e.description LIKE %:keyword%)")
    Page<ExerciseLibrary> searchByKeyword(@Param("keyword") String keyword, @Param("status") String status, Pageable pageable);

    /**
     * 组合查询：类别 + 难度 + 器械
     */
    @Query("SELECT e FROM ExerciseLibrary e WHERE e.status = :status " +
           "AND (:category IS NULL OR e.category = :category) " +
           "AND (:difficulty IS NULL OR e.difficulty = :difficulty) " +
           "AND (:equipment IS NULL OR e.equipment = :equipment)")
    Page<ExerciseLibrary> findByFilters(
        @Param("category") String category,
        @Param("difficulty") String difficulty,
        @Param("equipment") String equipment,
        @Param("status") String status,
        Pageable pageable
    );

    /**
     * 查询热门动作（按观看次数排序）
     */
    List<ExerciseLibrary> findTop10ByStatusOrderByViewCountDesc(String status);

    /**
     * 查询推荐动作（按点赞次数排序）
     */
    List<ExerciseLibrary> findTop10ByStatusOrderByLikeCountDesc(String status);
}
