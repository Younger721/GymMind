package com.gymmind.iam.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 320) String email,
        @NotBlank @Size(max = 72) String password) {

    public LoginRequest {
        email = email == null ? null : email.trim();
    }
}
