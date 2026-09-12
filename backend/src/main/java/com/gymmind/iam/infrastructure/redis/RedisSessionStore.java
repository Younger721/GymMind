package com.gymmind.iam.infrastructure.redis;

import com.gymmind.iam.application.model.RefreshSession;
import com.gymmind.iam.application.port.SessionStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Repository
public class RedisSessionStore implements SessionStore {

    private static final Pattern KEY = Pattern.compile(
            "gymmind:[a-z0-9][a-z0-9_-]*:v1:auth:(?:platform|[1-9][0-9]*):"
                    + "[1-9][0-9]*:(?:refresh|access):[A-Za-z0-9._~-]+");
    private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
    private static final DefaultRedisScript<Long> CONSUME_REFRESH = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if value and value == ARGV[1] then return redis.call('DEL', KEYS[1]); end; "
                    + "return 0;",
            Long.class);

    private final StringRedisTemplate redis;

    public RedisSessionStore(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis, "redis");
    }

    @Override
    public void storeRefresh(RefreshSession session, Duration ttl) {
        Objects.requireNonNull(session, "session");
        String key = requireKey(session.tokenId(), "refresh");
        String hash = requireHash(session.tokenHash());
        redis.opsForValue().set(key, hash, requirePositive(ttl));
    }

    @Override
    public boolean consumeRefresh(String namespacedTokenId, String tokenHash) {
        String key = requireKey(namespacedTokenId, "refresh");
        Long consumed = redis.execute(CONSUME_REFRESH, List.of(key), requireHash(tokenHash));
        return Long.valueOf(1L).equals(consumed);
    }

    @Override
    public void revokeAccess(String namespacedTokenId, Duration ttl) {
        redis.opsForValue().set(requireKey(namespacedTokenId, "access"), "revoked", requirePositive(ttl));
    }

    @Override
    public boolean isAccessRevoked(String namespacedTokenId) {
        return Boolean.TRUE.equals(redis.hasKey(requireKey(namespacedTokenId, "access")));
    }

    @Override
    public void deleteRefresh(String namespacedTokenId) {
        redis.delete(requireKey(namespacedTokenId, "refresh"));
    }

    private static String requireKey(String value, String kind) {
        if (value == null || !KEY.matcher(value).matches() || !value.contains(':' + kind + ':')) {
            throw new IllegalArgumentException("Invalid " + kind + " token key");
        }
        return value;
    }

    private static String requireHash(String value) {
        if (value == null || !SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException("Token hash must be a lowercase SHA-256 digest");
        }
        return value;
    }

    private static Duration requirePositive(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("Redis TTL must be positive");
        }
        return value;
    }
}
