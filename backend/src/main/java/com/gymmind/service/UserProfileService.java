package com.gymmind.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.common.exception.BusinessException;
import com.gymmind.dto.profile.UpdateProfileRequest;
import com.gymmind.dto.profile.UserProfileResponse;
import com.gymmind.entity.UserProfile;
import com.gymmind.repository.UserProfileRepository;
import com.gymmind.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    public UserProfileResponse getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(404, "User profile not found"));

        return toResponse(profile);
    }

    @Transactional
    public UserProfileResponse createOrUpdateProfile(UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();

        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElse(UserProfile.builder().userId(userId).build());

        updateProfileFields(profile, request);
        profile = userProfileRepository.save(profile);

        log.info("User profile updated for userId: {}", userId);
        return toResponse(profile);
    }

    private void updateProfileFields(UserProfile profile, UpdateProfileRequest request) {
        if (request.getHeight() != null) profile.setHeight(request.getHeight());
        if (request.getWeight() != null) profile.setWeight(request.getWeight());
        if (request.getTargetWeight() != null) profile.setTargetWeight(request.getTargetWeight());
        if (request.getBodyFatPercentage() != null) profile.setBodyFatPercentage(request.getBodyFatPercentage());
        if (request.getFitnessGoal() != null) profile.setFitnessGoal(request.getFitnessGoal());
        if (request.getExperienceLevel() != null) profile.setExperienceLevel(request.getExperienceLevel());
        if (request.getWeeklyWorkoutDays() != null) profile.setWeeklyWorkoutDays(request.getWeeklyWorkoutDays());
        if (request.getDailyWorkoutMinutes() != null) profile.setDailyWorkoutMinutes(request.getDailyWorkoutMinutes());
        if (request.getAvailableEquipment() != null) profile.setAvailableEquipment(toJson(request.getAvailableEquipment()));
        if (request.getDietaryPreference() != null) profile.setDietaryPreference(request.getDietaryPreference());
        if (request.getFoodAllergies() != null) profile.setFoodAllergies(toJson(request.getFoodAllergies()));
        if (request.getFoodDislikes() != null) profile.setFoodDislikes(toJson(request.getFoodDislikes()));
        if (request.getHealthConditions() != null) profile.setHealthConditions(request.getHealthConditions());
        if (request.getInjuries() != null) profile.setInjuries(request.getInjuries());
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .height(profile.getHeight())
                .weight(profile.getWeight())
                .targetWeight(profile.getTargetWeight())
                .bodyFatPercentage(profile.getBodyFatPercentage())
                .fitnessGoal(profile.getFitnessGoal())
                .experienceLevel(profile.getExperienceLevel())
                .weeklyWorkoutDays(profile.getWeeklyWorkoutDays())
                .dailyWorkoutMinutes(profile.getDailyWorkoutMinutes())
                .availableEquipment(fromJson(profile.getAvailableEquipment()))
                .dietaryPreference(profile.getDietaryPreference())
                .foodAllergies(fromJson(profile.getFoodAllergies()))
                .foodDislikes(fromJson(profile.getFoodDislikes()))
                .healthConditions(profile.getHealthConditions())
                .injuries(profile.getInjuries())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert list to JSON", e);
            return "[]";
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON", e);
            return List.of();
        }
    }
}
