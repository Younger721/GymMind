package com.gymmind.iam.api.request;

import com.gymmind.iam.application.command.InviteUserCommand;
import com.gymmind.iam.domain.model.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteUserRequest(@NotBlank @Email String email, @NotNull RoleCode role) {
    public InviteUserCommand toCommand() {
        return new InviteUserCommand(email, role);
    }
}
