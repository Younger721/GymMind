package com.gymmind.iam.application.result;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.jwt.TokenPair;

import java.util.Objects;
import java.util.Set;

public record AuthResult(AuthenticatedUser user, TokenPair tokens) {

    public AuthResult {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(tokens, "tokens");
    }

    public record AuthenticatedUser(
            Long id,
            Long tenantId,
            String email,
            String displayName,
            Set<RoleCode> roles,
            Set<String> permissions) {

        public AuthenticatedUser {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(email, "email");
            Objects.requireNonNull(displayName, "displayName");
            roles = Set.copyOf(Objects.requireNonNull(roles, "roles"));
            permissions = Set.copyOf(Objects.requireNonNull(permissions, "permissions"));
        }
    }
}
