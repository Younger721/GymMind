package com.gymmind.shared.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@ActiveProfiles("test")
@TestPropertySource(properties = "gymmind.environment=test")
@Import(RedisStreamPersistentJobQueue.class)
class RedisStreamPersistentJobQueueIT {

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

    @Autowired
    private StringRedisTemplate redis;

    private RedisStreamPersistentJobQueue queue;

    @BeforeEach
    void setUp() {
        redis.getConnectionFactory().getConnection().serverCommands().flushDb();
        queue = new RedisStreamPersistentJobQueue(redis, "test", 5, 30);
    }

    @Test
    void enqueueClaimCompleteRoundTrip() {
        Long id = queue.enqueue("KNOWLEDGE_INGEST:8", null);
        assertThat(id).isPositive();

        var claimed = queue.claimNext(Instant.parse("2026-01-01T00:00:00Z"), Duration.ofMinutes(5));
        assertThat(claimed).isPresent();
        assertThat(claimed.get().status()).isEqualTo(JobStatus.RUNNING);

        queue.complete(id);
        assertThat(queue.find(id).status()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void deduplicatesActiveJobForSameTypeAndTenant() {
        Long first = queue.enqueue("KNOWLEDGE_INGEST:9", 7L);
        Long second = queue.enqueue("KNOWLEDGE_INGEST:9", 7L);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void retriesWithBackoffAndRecoversExpiredLease() {
        Long id = queue.enqueue("KNOWLEDGE_INGEST:10", 7L);
        queue.claimNext(Instant.parse("2026-01-01T00:00:00Z"), Duration.ofMinutes(1));

        queue.recoverExpired(Instant.parse("2026-01-01T00:02:00Z"));
        assertThat(queue.find(id).status()).isEqualTo(JobStatus.PENDING);

        queue.retry(id, "boom", Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(queue.find(id).attempts()).isEqualTo(1);
        assertThat(queue.find(id).nextAttemptAt()).isEqualTo(Instant.parse("2026-01-01T00:00:30Z"));
    }
}
