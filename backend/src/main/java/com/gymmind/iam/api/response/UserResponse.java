package com.gymmind.iam.api.response;

import com.gymmind.iam.application.UserAdministrationService;
import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.iam.domain.model.UserStatus;

import java.util.Set;

public record UserResponse(Long id, Long tenantId, String email, String displayName,
                           UserStatus status, Set<RoleCode> roles) {
    public static UserResponse from(UserAdministrationService.UserView user) {
        return new UserResponse(user.id(), user.tenantId(), user.email(), user.displayName(), user.status(), user.roles());
    }
    public static UserResponse from(UserAdministrationService.UserSummary user) {
        return new UserResponse(user.id(), user.tenantId(), user.email(), user.displayName(), user.status(), user.roles());
    }
}
