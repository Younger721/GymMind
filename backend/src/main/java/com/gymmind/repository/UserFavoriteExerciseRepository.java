package com.gymmind.repository;

import com.gymmind.entity.UserFavoriteExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户收藏动作数据访问层
 */
@Repository
public interface UserFavoriteExerciseRepository extends JpaRepository<UserFavoriteExercise, Long> {

    /**
     * 查询用户的所有收藏
     */
    List<UserFavoriteExercise> findByUserId(Long userId);

    /**
     * 查询用户是否收藏了某个动作
     */
    Optional<UserFavoriteExercise> findByUserIdAndExerciseId(Long userId, Long exerciseId);

    /**
     * 删除用户的收藏
     */
    void deleteByUserIdAndExerciseId(Long userId, Long exerciseId);

    /**
     * 统计动作被收藏的次数
     */
    long countByExerciseId(Long exerciseId);
}
