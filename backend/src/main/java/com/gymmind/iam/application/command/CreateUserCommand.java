package com.gymmind.iam.application.command;

import com.gymmind.iam.domain.model.RoleCode;

public record CreateUserCommand(Long tenantId, String username, String email, String password, String displayName, String phone, RoleCode role, String membershipCardType) {
    public CreateUserCommand(String username, String email, String password, String displayName, RoleCode role) {
        this(null, username, email, password, displayName, null, role, null);
    }
}
