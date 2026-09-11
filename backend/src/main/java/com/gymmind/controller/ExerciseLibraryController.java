package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.exercise.ExerciseLibraryRequest;
import com.gymmind.dto.exercise.ExerciseLibraryResponse;
import com.gymmind.service.ExerciseLibraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 动作库控制器
 * 提供动作库的增删改查、收藏、推荐等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
public class ExerciseLibraryController {

    private final ExerciseLibraryService exerciseLibraryService;

    /**
     * 分页查询动作库（支持筛选和搜索）
     *
     * @param category 动作类别（可选）
     * @param difficulty 难度（可选）
     * @param equipment 器械（可选）
     * @param keyword 搜索关键词（可选）
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param sortBy 排序方式：popular（热门）/liked（点赞）/newest（最新）/default（默认）
     * @return 分页结果
     */
    @GetMapping
    public ApiResponse<Page<ExerciseLibraryResponse>> getExercises(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String equipment,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "default") String sortBy
    ) {
        log.info("查询动作库: category={}, difficulty={}, equipment={}, keyword={}, page={}, size={}, sortBy={}",
                category, difficulty, equipment, keyword, page, size, sortBy);

        Page<ExerciseLibraryResponse> result = exerciseLibraryService.getExercises(
                category, difficulty, equipment, keyword, page, size, sortBy
        );

        return ApiResponse.success(result);
    }

    /**
     * 根据ID查询动作详情
     *
     * @param id 动作ID
     * @return 动作详情
     */
    @GetMapping("/{id}")
    public ApiResponse<ExerciseLibraryResponse> getExerciseById(@PathVariable Long id) {
        log.info("查询动作详情: id={}", id);
        ExerciseLibraryResponse exercise = exerciseLibraryService.getExerciseById(id);
        return ApiResponse.success(exercise);
    }

    /**
     * 创建动作
     *
     * @param request 动作信息
     * @return 创建的动作
     */
    @PostMapping
    public ApiResponse<ExerciseLibraryResponse> createExercise(@RequestBody ExerciseLibraryRequest request) {
        log.info("创建动作: {}", request.getName());
        ExerciseLibraryResponse exercise = exerciseLibraryService.createExercise(request);
        return ApiResponse.success(exercise);
    }

    /**
     * 更新动作
     *
     * @param id 动作ID
     * @param request 更新信息
     * @return 更新后的动作
     */
    @PutMapping("/{id}")
    public ApiResponse<ExerciseLibraryResponse> updateExercise(
            @PathVariable Long id,
            @RequestBody ExerciseLibraryRequest request
    ) {
        log.info("更新动作: id={}", id);
        ExerciseLibraryResponse exercise = exerciseLibraryService.updateExercise(id, request);
        return ApiResponse.success(exercise);
    }

    /**
     * 删除动作（软删除）
     *
     * @param id 动作ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteExercise(@PathVariable Long id) {
        log.info("删除动作: id={}", id);
        exerciseLibraryService.deleteExercise(id);
        return ApiResponse.success(null);
    }

    /**
     * 收藏/取消收藏动作
     *
     * @param id 动作ID
     * @return 成功响应
     */
    @PostMapping("/{id}/favorite")
    public ApiResponse<Void> toggleFavorite(@PathVariable Long id) {
        log.info("切换收藏状态: exerciseId={}", id);
        exerciseLibraryService.toggleFavorite(id);
        return ApiResponse.success(null);
    }

    /**
     * 查询当前用户收藏的动作
     *
     * @return 收藏列表
     */
    @GetMapping("/favorites")
    public ApiResponse<List<ExerciseLibraryResponse>> getUserFavorites() {
        log.info("查询用户收藏");
        List<ExerciseLibraryResponse> favorites = exerciseLibraryService.getUserFavorites();
        return ApiResponse.success(favorites);
    }

    /**
     * 查询热门动作（Top 10）
     *
     * @return 热门动作列表
     */
    @GetMapping("/popular")
    public ApiResponse<List<ExerciseLibraryResponse>> getPopularExercises() {
        log.info("查询热门动作");
        List<ExerciseLibraryResponse> exercises = exerciseLibraryService.getPopularExercises();
        return ApiResponse.success(exercises);
    }

    /**
     * 查询推荐动作（Top 10）
     *
     * @return 推荐动作列表
     */
    @GetMapping("/recommended")
    public ApiResponse<List<ExerciseLibraryResponse>> getRecommendedExercises() {
        log.info("查询推荐动作");
        List<ExerciseLibraryResponse> exercises = exerciseLibraryService.getRecommendedExercises();
        return ApiResponse.success(exercises);
    }
}
