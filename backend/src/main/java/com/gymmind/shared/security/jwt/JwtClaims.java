package com.gymmind.shared.security.jwt;

import com.gymmind.iam.domain.model.RoleCode;
import com.gymmind.shared.security.CurrentActor;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record JwtClaims(
        String issuer,
        Long userId,
        Long tenantId,
        Set<RoleCode> roles,
        Set<String> permissions,
        long tokenVersion,
        String tokenId,
        TokenType type,
        Instant issuedAt,
        Instant expiresAt) {

    public JwtClaims {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("Issuer must not be blank");
        }
        issuer = issuer.trim();
        CurrentActor actor = new CurrentActor(
                userId, tenantId, roles, permissions, tokenVersion, tokenId);
        roles = actor.roles();
        permissions = actor.permissions();
        tokenId = actor.tokenId();
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(issuedAt, "issuedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("Expiration must be after issue time");
        }
    }

    public CurrentActor toCurrentActor() {
        return new CurrentActor(userId, tenantId, roles, permissions, tokenVersion, tokenId);
    }
}
