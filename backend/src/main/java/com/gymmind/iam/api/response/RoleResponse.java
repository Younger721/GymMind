package com.gymmind.iam.api.response;

import com.gymmind.iam.application.UserAdministrationService;
import com.gymmind.iam.domain.model.RoleCode;

public record RoleResponse(Long id, RoleCode code, String name) {
    public static RoleResponse from(UserAdministrationService.RoleView role) {
        return new RoleResponse(role.id(), role.code(), role.name());
    }
}
