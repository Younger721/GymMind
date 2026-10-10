package com.gymmind.iam.api.response;

import com.gymmind.iam.application.UserAdministrationService;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserStatus;

import java.util.Set;

public record UserResponse(Long id, Long tenantId, String tenantName, String username, String email, String displayName, String phone,
                           UserStatus status, Set<RoleCode> roles) {
    public static UserResponse from(UserAdministrationService.UserView user) {
        return new UserResponse(user.id(), user.tenantId(), null, user.username(), user.email(), user.displayName(), user.phone(),
                user.status(), user.roles());
    }
    public static UserResponse from(UserAdministrationService.UserSummary user) {
        return new UserResponse(user.id(), user.tenantId(), user.tenantName(), user.username(), user.email(), user.displayName(), user.phone(),
                user.status(), user.roles());
    }
}
