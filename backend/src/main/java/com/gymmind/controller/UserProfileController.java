package com.gymmind.controller;

import com.gymmind.common.response.ApiResponse;
import com.gymmind.dto.profile.UpdateProfileRequest;
import com.gymmind.dto.profile.UserProfileResponse;
import com.gymmind.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public ApiResponse<UserProfileResponse> getProfile() {
        UserProfileResponse profile = userProfileService.getProfile();
        return ApiResponse.success(profile);
    }

    @PutMapping
    public ApiResponse<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse profile = userProfileService.createOrUpdateProfile(request);
        return ApiResponse.success("Profile updated successfully", profile);
    }
}
