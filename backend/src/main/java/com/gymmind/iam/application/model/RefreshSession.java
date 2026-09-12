package com.gymmind.iam.application.model;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

public record RefreshSession(String tokenId, String tokenHash, Instant expiresAt) {

    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

    public RefreshSession {
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("Token id must not be blank");
        }
        tokenId = tokenId.trim();
        expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (tokenHash != null && !SHA_256.matcher(tokenHash).matches()) {
            throw new IllegalArgumentException("Token hash must be a lowercase SHA-256 digest");
        }
    }

    public static RefreshSession pending(String namespacedTokenId, Instant expiresAt) {
        return new RefreshSession(namespacedTokenId, null, expiresAt);
    }

    public static RefreshSession stored(String namespacedTokenId, String tokenHash, Instant expiresAt) {
        if (tokenHash == null) {
            throw new IllegalArgumentException("Token hash must not be null");
        }
        return new RefreshSession(namespacedTokenId, tokenHash, expiresAt);
    }

    public RefreshSession withTokenHash(String value) {
        return stored(tokenId, value, expiresAt);
    }
}
