package com.gymmind.iam.infrastructure;

import com.gymmind.iam.application.model.RefreshSession;
import com.gymmind.iam.application.port.SessionStore;
import com.gymmind.iam.infrastructure.redis.RedisSessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@ActiveProfiles("test")
@Import(RedisSessionStore.class)
class RedisSessionStoreIT {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    static {
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired private SessionStore store;
    @Autowired private StringRedisTemplate redis;

    @BeforeEach
    void clearRedis() {
        redis.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    void storesDigestWithTtlAndAtomicallyConsumesOnlyOnce() throws Exception {
        String key = "gymmind:test:v1:auth:7:42:refresh:refresh-jti";
        String hash = "557888b8721a0f0bff6ef70bcfd0032d0aa38fd0e9057b40afee31925d1fc762";
        store.storeRefresh(RefreshSession.stored(key, hash, Instant.now().plusSeconds(30)),
                Duration.ofSeconds(30));

        assertThat(redis.opsForValue().get(key)).isEqualTo(hash);
        assertThat(redis.getExpire(key, TimeUnit.SECONDS)).isBetween(1L, 30L);
        assertThat(store.consumeRefresh(key, "0".repeat(64))).isFalse();
        assertThat(redis.hasKey(key)).isTrue();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Boolean>> attempts = List.of(
                    () -> store.consumeRefresh(key, hash),
                    () -> store.consumeRefresh(key, hash));
            List<Boolean> results = new ArrayList<>();
            executor.invokeAll(attempts).forEach(future -> {
                try {
                    results.add(future.get());
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            });
            assertThat(results).containsExactlyInAnyOrder(true, false);
            assertThat(redis.hasKey(key)).isFalse();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void atomicallyRevokesAccessWithTtlAndDeletesRefreshSession() {
        String accessKey = "gymmind:test:v1:auth:platform:1:access:access-jti";
        String refreshKey = "gymmind:test:v1:auth:platform:1:refresh:refresh-jti";
        String hash = "a".repeat(64);
        store.storeRefresh(RefreshSession.stored(refreshKey, hash, Instant.now().plusSeconds(30)),
                Duration.ofSeconds(30));

        store.revokeAccessAndDeleteRefresh(accessKey, Duration.ofSeconds(30), refreshKey);
        assertThat(store.isAccessRevoked(accessKey)).isTrue();
        assertThat(redis.getExpire(accessKey, TimeUnit.SECONDS)).isBetween(1L, 30L);
        assertThat(redis.hasKey(refreshKey)).isFalse();
    }
}
