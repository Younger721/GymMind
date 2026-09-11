package com.gymmind.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.common.exception.BusinessException;
import com.gymmind.dto.exercise.ExerciseLibraryRequest;
import com.gymmind.dto.exercise.ExerciseLibraryResponse;
import com.gymmind.entity.ExerciseLibrary;
import com.gymmind.entity.UserFavoriteExercise;
import com.gymmind.repository.ExerciseLibraryRepository;
import com.gymmind.repository.UserFavoriteExerciseRepository;
import com.gymmind.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 动作库服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseLibraryService {

    private final ExerciseLibraryRepository exerciseLibraryRepository;
    private final UserFavoriteExerciseRepository favoriteRepository;
    private final ObjectMapper objectMapper;

    /**
     * 分页查询动作库（支持筛选）
     */
    public Page<ExerciseLibraryResponse> getExercises(
            String category,
            String difficulty,
            String equipment,
            String keyword,
            int page,
            int size,
            String sortBy
    ) {
        // 构建分页和排序
        Sort sort = buildSort(sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ExerciseLibrary> exercisePage;

        // 根据搜索条件查询
        if (keyword != null && !keyword.trim().isEmpty()) {
            exercisePage = exerciseLibraryRepository.searchByKeyword(keyword, "ACTIVE", pageable);
        } else if (category != null || difficulty != null || equipment != null) {
            exercisePage = exerciseLibraryRepository.findByFilters(category, difficulty, equipment, "ACTIVE", pageable);
        } else {
            exercisePage = exerciseLibraryRepository.findByStatus("ACTIVE", pageable);
        }

        // 转换为DTO
        return exercisePage.map(this::convertToResponse);
    }

    /**
     * 根据ID查询动作详情
     */
    @Transactional
    public ExerciseLibraryResponse getExerciseById(Long id) {
        ExerciseLibrary exercise = exerciseLibraryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("动作不存在"));

        // 增加观看次数
        exercise.setViewCount(exercise.getViewCount() + 1);
        exerciseLibraryRepository.save(exercise);

        return convertToResponse(exercise);
    }

    /**
     * 创建动作
     */
    @Transactional
    public ExerciseLibraryResponse createExercise(ExerciseLibraryRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        ExerciseLibrary exercise = new ExerciseLibrary();
        exercise.setName(request.getName());
        exercise.setNameEn(request.getNameEn());
        exercise.setCategory(request.getCategory());
        exercise.setDifficulty(request.getDifficulty());
        exercise.setEquipment(request.getEquipment());
        exercise.setVideoUrl(request.getVideoUrl());
        exercise.setVideoThumbnail(request.getVideoThumbnail());
        exercise.setVideoDuration(request.getVideoDuration());
        exercise.setVideoSource(request.getVideoSource());
        exercise.setDescription(request.getDescription());
        exercise.setTargetMuscles(request.getTargetMuscles());
        exercise.setCreatedBy(currentUserId);

        // 转换JSON字段
        try {
            if (request.getInstructions() != null) {
                exercise.setInstructions(objectMapper.writeValueAsString(request.getInstructions()));
            }
            if (request.getTips() != null) {
                exercise.setTips(objectMapper.writeValueAsString(request.getTips()));
            }
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            throw new BusinessException("数据格式错误");
        }

        ExerciseLibrary saved = exerciseLibraryRepository.save(exercise);
        return convertToResponse(saved);
    }

    /**
     * 更新动作
     */
    @Transactional
    public ExerciseLibraryResponse updateExercise(Long id, ExerciseLibraryRequest request) {
        ExerciseLibrary exercise = exerciseLibraryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("动作不存在"));

        exercise.setName(request.getName());
        exercise.setNameEn(request.getNameEn());
        exercise.setCategory(request.getCategory());
        exercise.setDifficulty(request.getDifficulty());
        exercise.setEquipment(request.getEquipment());
        exercise.setVideoUrl(request.getVideoUrl());
        exercise.setVideoThumbnail(request.getVideoThumbnail());
        exercise.setVideoDuration(request.getVideoDuration());
        exercise.setVideoSource(request.getVideoSource());
        exercise.setDescription(request.getDescription());
        exercise.setTargetMuscles(request.getTargetMuscles());

        try {
            if (request.getInstructions() != null) {
                exercise.setInstructions(objectMapper.writeValueAsString(request.getInstructions()));
            }
            if (request.getTips() != null) {
                exercise.setTips(objectMapper.writeValueAsString(request.getTips()));
            }
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            throw new BusinessException("数据格式错误");
        }

        ExerciseLibrary saved = exerciseLibraryRepository.save(exercise);
        return convertToResponse(saved);
    }

    /**
     * 删除动作
     */
    @Transactional
    public void deleteExercise(Long id) {
        ExerciseLibrary exercise = exerciseLibraryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("动作不存在"));

        exercise.setStatus("INACTIVE");
        exerciseLibraryRepository.save(exercise);
    }

    /**
     * 收藏/取消收藏动作
     */
    @Transactional
    public void toggleFavorite(Long exerciseId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 检查动作是否存在
        exerciseLibraryRepository.findById(exerciseId)
                .orElseThrow(() -> new BusinessException("动作不存在"));

        // 查询是否已收藏
        var favorite = favoriteRepository.findByUserIdAndExerciseId(currentUserId, exerciseId);

        if (favorite.isPresent()) {
            // 取消收藏
            favoriteRepository.delete(favorite.get());
        } else {
            // 添加收藏
            UserFavoriteExercise newFavorite = new UserFavoriteExercise();
            newFavorite.setUserId(currentUserId);
            newFavorite.setExerciseId(exerciseId);
            favoriteRepository.save(newFavorite);
        }
    }

    /**
     * 查询用户收藏的动作
     */
    public List<ExerciseLibraryResponse> getUserFavorites() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        List<UserFavoriteExercise> favorites = favoriteRepository.findByUserId(currentUserId);
        List<Long> exerciseIds = favorites.stream()
                .map(UserFavoriteExercise::getExerciseId)
                .collect(Collectors.toList());

        if (exerciseIds.isEmpty()) {
            return new ArrayList<>();
        }

        return exerciseLibraryRepository.findAllById(exerciseIds).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 查询热门动作
     */
    public List<ExerciseLibraryResponse> getPopularExercises() {
        return exerciseLibraryRepository.findTop10ByStatusOrderByViewCountDesc("ACTIVE").stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 查询推荐动作
     */
    public List<ExerciseLibraryResponse> getRecommendedExercises() {
        return exerciseLibraryRepository.findTop10ByStatusOrderByLikeCountDesc("ACTIVE").stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 转换为响应DTO
     */
    private ExerciseLibraryResponse convertToResponse(ExerciseLibrary exercise) {
        ExerciseLibraryResponse response = new ExerciseLibraryResponse();
        response.setId(exercise.getId());
        response.setName(exercise.getName());
        response.setNameEn(exercise.getNameEn());
        response.setCategory(exercise.getCategory());
        response.setDifficulty(exercise.getDifficulty());
        response.setEquipment(exercise.getEquipment());
        response.setVideoUrl(exercise.getVideoUrl());
        response.setVideoThumbnail(exercise.getVideoThumbnail());
        response.setVideoDuration(exercise.getVideoDuration());
        response.setVideoSource(exercise.getVideoSource());
        response.setDescription(exercise.getDescription());
        response.setTargetMuscles(exercise.getTargetMuscles());
        response.setViewCount(exercise.getViewCount());
        response.setLikeCount(exercise.getLikeCount());
        response.setIsVerified(exercise.getIsVerified());
        response.setCreatedAt(exercise.getCreatedAt());
        response.setUpdatedAt(exercise.getUpdatedAt());

        // 解析JSON字段
        try {
            if (exercise.getInstructions() != null) {
                response.setInstructions(objectMapper.readValue(
                    exercise.getInstructions(),
                    new TypeReference<List<String>>() {}
                ));
            }
            if (exercise.getTips() != null) {
                response.setTips(objectMapper.readValue(
                    exercise.getTips(),
                    new TypeReference<List<String>>() {}
                ));
            }
        } catch (JsonProcessingException e) {
            log.error("JSON反序列化失败", e);
        }

        // 检查当前用户是否已收藏
        Long currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        if (currentUserId != null) {
            boolean isFavorited = favoriteRepository
                    .findByUserIdAndExerciseId(currentUserId, exercise.getId())
                    .isPresent();
            response.setIsFavorited(isFavorited);
        } else {
            response.setIsFavorited(false);
        }

        return response;
    }

    /**
     * 构建排序条件
     */
    private Sort buildSort(String sortBy) {
        return switch (sortBy) {
            case "popular" -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "liked" -> Sort.by(Sort.Direction.DESC, "likeCount");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            default -> Sort.by(Sort.Direction.ASC, "name");
        };
    }
}
