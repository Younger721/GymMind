package com.gymmind.iam.application.command;

import com.gymmind.iam.domain.model.RoleCode;

public record CreateUserCommand(String email, String password, String displayName, RoleCode role) {
}
