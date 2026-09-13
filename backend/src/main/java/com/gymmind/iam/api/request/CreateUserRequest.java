package com.gymmind.iam.api.request;

import com.gymmind.iam.application.command.CreateUserCommand;
import com.gymmind.iam.domain.model.RoleCode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 128) String displayName,
        @NotNull RoleCode role) {
    public CreateUserCommand toCommand() {
        return new CreateUserCommand(email, password, displayName, role);
    }
}
