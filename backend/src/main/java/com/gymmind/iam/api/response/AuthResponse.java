package com.gymmind.iam.api.response;

import com.gymmind.iam.application.result.AuthResult;
import com.gymmind.iam.domain.model.RoleCode;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record AuthResponse(
        Long userId,
        Long tenantId,
        String email,
        String displayName,
        Set<RoleCode> roles,
        Set<String> permissions,
        String accessToken,
        Instant accessExpiresAt,
        String refreshToken,
        Instant refreshExpiresAt) {

    public static AuthResponse from(AuthResult result) {
        Objects.requireNonNull(result, "result");
        return new AuthResponse(
                result.user().id(),
                result.user().tenantId(),
                result.user().email(),
                result.user().displayName(),
                result.user().roles(),
                result.user().permissions(),
                result.tokens().accessToken(),
                result.tokens().accessExpiresAt(),
                result.tokens().refreshToken(),
                result.tokens().refreshExpiresAt());
    }
}
