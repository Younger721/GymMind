package com.gymmind.iam.application;

import com.gymmind.iam.application.model.RefreshSession;
import com.gymmind.iam.application.port.SessionStore;
import com.gymmind.shared.error.BusinessException;
import com.gymmind.shared.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@Service
public class SessionService {

    private static final Pattern ENVIRONMENT = Pattern.compile("[a-z0-9][a-z0-9_-]*");
    private static final Pattern TOKEN_ID = Pattern.compile("[A-Za-z0-9._~-]+");

    private final SessionStore store;
    private final Clock clock;
    private final String environment;

    public SessionService(
            SessionStore store,
            Clock clock,
            @Value("${gymmind.environment:${spring.profiles.active:dev}}") String environment) {
        this.store = Objects.requireNonNull(store, "store");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.environment = requireEnvironment(environment);
    }

    public RefreshSession refreshSession(Long tenantId, Long userId, String tokenId, Instant expiresAt) {
        return RefreshSession.pending(refreshTokenId(tenantId, userId, tokenId), expiresAt);
    }

    public void store(RefreshSession session, String rawRefreshToken) {
        Objects.requireNonNull(session, "session");
        String tokenHash = sha256(rawRefreshToken);
        Duration ttl = remainingTtl(session.expiresAt());
        runStore(() -> store.storeRefresh(session.withTokenHash(tokenHash), ttl));
    }

    public boolean consume(String namespacedTokenId, String rawRefreshToken) {
        String tokenId = requireNamespacedTokenId(namespacedTokenId, "refresh");
        String tokenHash = sha256(rawRefreshToken);
        return callStore(() -> store.consumeRefresh(tokenId, tokenHash));
    }

    public void deleteRefresh(String namespacedTokenId) {
        String tokenId = requireNamespacedTokenId(namespacedTokenId, "refresh");
        runStore(() -> store.deleteRefresh(tokenId));
    }

    public void revokeAccess(Long tenantId, Long userId, String tokenId, Instant expiresAt) {
        String namespacedTokenId = accessTokenId(tenantId, userId, tokenId);
        Duration ttl = remainingTtl(expiresAt);
        runStore(() -> store.revokeAccess(namespacedTokenId, ttl));
    }

    public boolean isAccessRevoked(Long tenantId, Long userId, String tokenId) {
        String namespacedTokenId = accessTokenId(tenantId, userId, tokenId);
        return callStore(() -> store.isAccessRevoked(namespacedTokenId));
    }

    public String refreshTokenId(Long tenantId, Long userId, String tokenId) {
        return namespacedTokenId(tenantId, userId, "refresh", tokenId);
    }

    public String accessTokenId(Long tenantId, Long userId, String tokenId) {
        return namespacedTokenId(tenantId, userId, "access", tokenId);
    }

    private String namespacedTokenId(Long tenantId, Long userId, String kind, String tokenId) {
        if (tenantId != null && tenantId <= 0) {
            throw new IllegalArgumentException("Tenant id must be positive");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User id must be positive");
        }
        String safeTokenId = requireTokenId(tokenId);
        String tenantScope = tenantId == null ? "platform" : tenantId.toString();
        return "gymmind:" + environment + ":v1:auth:" + tenantScope + ':'
                + userId + ':' + kind + ':' + safeTokenId;
    }

    private Duration remainingTtl(Instant expiresAt) {
        Objects.requireNonNull(expiresAt, "expiresAt");
        Duration ttl = Duration.between(clock.instant(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Token expiry must be in the future");
        }
        return ttl;
    }

    private static String sha256(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token must not be blank");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable");
        }
    }

    private static String requireEnvironment(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Environment must not be blank");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!ENVIRONMENT.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Environment contains unsupported characters");
        }
        return normalized;
    }

    private static String requireTokenId(String value) {
        if (value == null || !TOKEN_ID.matcher(value).matches()) {
            throw new IllegalArgumentException("Token id contains unsupported characters");
        }
        return value;
    }

    private static String requireNamespacedTokenId(String value, String kind) {
        if (value == null || value.isBlank() || !value.contains(':' + kind + ':')) {
            throw new IllegalArgumentException("Invalid namespaced token id");
        }
        return value;
    }

    private void runStore(Runnable operation) {
        callStore(() -> {
            operation.run();
            return null;
        });
    }

    private <T> T callStore(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE);
        }
    }
}
