package com.gymmind.iam.application.command;

import com.gymmind.iam.domain.model.RoleCode;

public record InviteUserCommand(String email, RoleCode role) {
}
