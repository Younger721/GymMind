package com.gymmind.iam.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 64) String tenantCode,
        @NotBlank @Size(max = 128) String tenantName,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 128) String displayName) {

    public RegisterRequest {
        email = email == null ? null : email.trim();
    }
}
