package com.gymmind.shared.async;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 基于 Redis Stream 的持久化任务队列，语义对齐 {@link MySqlPersistentJobQueue}。
 * 任务元数据保存在 Hash，Stream 仅负责唤醒 worker；延迟重试使用 ZSET。
 */
@Component
@ConditionalOnProperty(name = "gymmind.job-queue", havingValue = "redis")
public class RedisStreamPersistentJobQueue implements PersistentJobQueue {

    private static final String FIELD_TYPE = "type";
    private static final String FIELD_TENANT = "tenantId";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ATTEMPTS = "attempts";
    private static final String FIELD_NEXT = "nextAttemptAt";
    private static final String FIELD_LEASE = "leaseUntil";
    private static final String FIELD_ERROR = "lastError";
    private static final String FIELD_STREAM_MSG = "streamMessageId";
    private static final String STREAM_FIELD_JOB_ID = "jobId";

    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]); end return 0;",
            Long.class);

    private final StringRedisTemplate redis;
    private final String streamKey;
    private final String groupName;
    private final String consumerName;
    private final String jobKeyPrefix;
    private final String slotKeyPrefix;
    private final String delayKey;
    private final String runningKey;
    private final String seqKey;
    private final String lockPrefix;
    private final int maxAttempts;
    private final long retryBaseSeconds;
    private volatile boolean groupReady;

    public RedisStreamPersistentJobQueue(
            StringRedisTemplate redis,
            @Value("${gymmind.environment:dev}") String environment,
            @Value("${gymmind.knowledge.ingest.max-attempts:5}") int maxAttempts,
            @Value("${gymmind.knowledge.ingest.retry-base-seconds:30}") long retryBaseSeconds) {
        this.redis = Objects.requireNonNull(redis, "redis");
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (retryBaseSeconds < 1) {
            throw new IllegalArgumentException("retryBaseSeconds must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.retryBaseSeconds = retryBaseSeconds;
        String env = environment == null || environment.isBlank() ? "dev" : environment.trim();
        String prefix = "gymmind:" + env + ":v1:async-job";
        this.streamKey = prefix + ":stream";
        this.groupName = "gymmind-workers";
        this.consumerName = "ingest-" + UUID.randomUUID();
        this.jobKeyPrefix = prefix + ":id:";
        this.slotKeyPrefix = prefix + ":slot:";
        this.delayKey = prefix + ":delay";
        this.runningKey = prefix + ":running";
        this.seqKey = prefix + ":seq";
        this.lockPrefix = prefix + ":lock:";
    }

    @Override
    public Long enqueue(String type, Long tenantId) {
        validate(type, tenantId);
        String normalizedType = type.trim();
        long queueTenant = queueTenant(tenantId);
        String lockKey = lockPrefix + queueTenant + ":" + normalizedType;
        String token = UUID.randomUUID().toString();
        if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(lockKey, token, Duration.ofSeconds(10)))) {
            throw new IllegalStateException("Unable to acquire job enqueue lock");
        }
        try {
            String slotKey = slotKey(queueTenant, normalizedType);
            String existingId = redis.opsForValue().get(slotKey);
            if (existingId != null && !existingId.isBlank()) {
                long jobId = Long.parseLong(existingId);
                PersistentJob current = loadJob(jobId);
                if (current.status() == JobStatus.PENDING || current.status() == JobStatus.RUNNING) {
                    return jobId;
                }
                if (current.status() == JobStatus.COMPLETED || current.status() == JobStatus.FAILED) {
                    resetForReplay(jobId, normalizedType, queueTenant);
                    publishReady(jobId);
                    return jobId;
                }
            }
            long jobId = nextJobId();
            PersistentJob created = new PersistentJob(jobId, normalizedType, tenantFromQueue(queueTenant),
                    JobStatus.PENDING, 0, Instant.EPOCH, null, null);
            saveJob(created, null);
            redis.opsForValue().set(slotKey, String.valueOf(jobId));
            publishReady(jobId);
            return jobId;
        } finally {
            redis.execute(RELEASE_LOCK, List.of(lockKey), token);
        }
    }

    @Override
    public Optional<PersistentJob> claimNext(Instant now, Duration lease) {
        if (now == null || lease == null || lease.isNegative() || lease.isZero()) {
            throw new IllegalArgumentException("invalid lease");
        }
        promoteDelayed(now);
        ensureGroup();
        for (int i = 0; i < 8; i++) {
            List<MapRecord<String, Object, Object>> batch = redis.opsForStream().read(
                    Consumer.from(groupName, consumerName),
                    StreamReadOptions.empty().count(1).block(Duration.ofMillis(50)),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed()));
            if (batch == null || batch.isEmpty()) {
                return Optional.empty();
            }
            MapRecord<String, Object, Object> record = batch.get(0);
            Object rawJobId = record.getValue().get(STREAM_FIELD_JOB_ID);
            if (rawJobId == null) {
                acknowledge(record.getId());
                continue;
            }
            long jobId = Long.parseLong(rawJobId.toString());
            PersistentJob job;
            try {
                job = loadJob(jobId);
            } catch (NoSuchElementException ex) {
                acknowledge(record.getId());
                continue;
            }
            if (job.status() != JobStatus.PENDING) {
                acknowledge(record.getId());
                continue;
            }
            if (job.nextAttemptAt() != null && job.nextAttemptAt().isAfter(now)) {
                scheduleDelay(jobId, job.nextAttemptAt());
                acknowledge(record.getId());
                continue;
            }
            Instant leaseUntil = now.plus(lease);
            PersistentJob running = new PersistentJob(job.id(), job.type(), job.tenantId(), JobStatus.RUNNING,
                    job.attempts(), job.nextAttemptAt(), leaseUntil, job.lastError());
            saveJob(running, record.getId().getValue());
            redis.opsForSet().add(runningKey, String.valueOf(jobId));
            return Optional.of(running);
        }
        return Optional.empty();
    }

    @Override
    public void advance(Long id) {
        PersistentJob job = loadJob(id);
        saveJob(new PersistentJob(job.id(), job.type(), job.tenantId(), JobStatus.RUNNING, job.attempts(),
                job.nextAttemptAt(), job.leaseUntil(), job.lastError()), streamMessageId(id));
    }

    @Override
    public void retry(Long id, String error, Instant now) {
        PersistentJob job = loadJob(id);
        acknowledgeIfPresent(id);
        redis.opsForSet().remove(runningKey, String.valueOf(id));
        int attempts = job.attempts() + 1;
        JobStatus status = attempts >= maxAttempts ? JobStatus.FAILED : JobStatus.PENDING;
        Instant next = status == JobStatus.FAILED ? job.nextAttemptAt()
                : now.plusSeconds(retryBaseSeconds << (attempts - 1));
        PersistentJob updated = new PersistentJob(job.id(), job.type(), job.tenantId(), status, attempts, next,
                null, error);
        saveJob(updated, null);
        if (status == JobStatus.PENDING) {
            if (next.isAfter(now)) {
                scheduleDelay(id, next);
            } else {
                publishReady(id);
            }
        }
    }

    @Override
    public void complete(Long id) {
        PersistentJob job = loadJob(id);
        acknowledgeIfPresent(id);
        redis.opsForSet().remove(runningKey, String.valueOf(id));
        saveJob(new PersistentJob(job.id(), job.type(), job.tenantId(), JobStatus.COMPLETED, job.attempts(),
                job.nextAttemptAt(), null, job.lastError()), null);
    }

    @Override
    public void recoverExpired(Instant now) {
        for (String member : redis.opsForSet().members(runningKey)) {
            if (member == null || member.isBlank()) {
                continue;
            }
            long jobId = Long.parseLong(member);
            PersistentJob job;
            try {
                job = loadJob(jobId);
            } catch (NoSuchElementException ex) {
                redis.opsForSet().remove(runningKey, member);
                continue;
            }
            if (job.status() != JobStatus.RUNNING || job.leaseUntil() == null || job.leaseUntil().isAfter(now)) {
                continue;
            }
            acknowledgeIfPresent(jobId);
            redis.opsForSet().remove(runningKey, member);
            PersistentJob pending = new PersistentJob(job.id(), job.type(), job.tenantId(), JobStatus.PENDING,
                    job.attempts(), Instant.EPOCH, null, job.lastError());
            saveJob(pending, null);
            publishReady(jobId);
        }
    }

    @Override
    public PersistentJob find(Long id) {
        return loadJob(id);
    }

    private void resetForReplay(long jobId, String type, long queueTenant) {
        saveJob(new PersistentJob(jobId, type, tenantFromQueue(queueTenant), JobStatus.PENDING, 0, Instant.EPOCH,
                null, null), null);
    }

    private void promoteDelayed(Instant now) {
        var ready = redis.opsForZSet().rangeByScore(delayKey, 0, now.toEpochMilli());
        if (ready == null || ready.isEmpty()) {
            return;
        }
        for (String jobId : ready) {
            redis.opsForZSet().remove(delayKey, jobId);
            publishReady(Long.parseLong(jobId));
        }
    }

    private void scheduleDelay(long jobId, Instant when) {
        redis.opsForZSet().add(delayKey, String.valueOf(jobId), when.toEpochMilli());
    }

    private void publishReady(long jobId) {
        redis.opsForStream().add(MapRecord.create(streamKey, Map.of(STREAM_FIELD_JOB_ID, String.valueOf(jobId))));
    }

    private void ensureGroup() {
        if (groupReady) {
            return;
        }
        synchronized (this) {
            if (groupReady) {
                return;
            }
            try {
                redis.opsForStream().createGroup(streamKey, ReadOffset.from("0"), groupName);
            } catch (RuntimeException ex) {
                if (!isBusyGroup(ex)) {
                    throw ex;
                }
            }
            groupReady = true;
        }
    }

    private void acknowledgeIfPresent(long jobId) {
        String messageId = streamMessageId(jobId);
        if (messageId != null && !messageId.isBlank()) {
            acknowledge(RecordId.of(messageId));
            redis.opsForHash().delete(jobKey(jobId), FIELD_STREAM_MSG);
        }
    }

    private void acknowledge(RecordId recordId) {
        redis.opsForStream().acknowledge(streamKey, groupName, recordId);
    }

    private String streamMessageId(long jobId) {
        Object value = redis.opsForHash().get(jobKey(jobId), FIELD_STREAM_MSG);
        return value == null ? null : value.toString();
    }

    private long nextJobId() {
        Long id = redis.opsForValue().increment(seqKey);
        if (id == null || id <= 0) {
            throw new IllegalStateException("Unable to allocate job id");
        }
        return id;
    }

    private PersistentJob loadJob(long id) {
        Map<Object, Object> raw = redis.opsForHash().entries(jobKey(id));
        if (raw == null || raw.isEmpty()) {
            throw new NoSuchElementException("job");
        }
        long tenant = Long.parseLong(String.valueOf(raw.get(FIELD_TENANT)));
        Instant next = parseInstant(raw.get(FIELD_NEXT));
        Instant lease = parseInstant(raw.get(FIELD_LEASE));
        return new PersistentJob(
                id,
                String.valueOf(raw.get(FIELD_TYPE)),
                tenantFromQueue(tenant),
                JobStatus.valueOf(String.valueOf(raw.get(FIELD_STATUS))),
                Integer.parseInt(String.valueOf(raw.get(FIELD_ATTEMPTS))),
                next,
                lease,
                raw.get(FIELD_ERROR) == null ? null : String.valueOf(raw.get(FIELD_ERROR)));
    }

    private void saveJob(PersistentJob job, String streamMessageId) {
        var hash = redis.opsForHash();
        String key = jobKey(job.id());
        hash.put(key, FIELD_TYPE, job.type());
        hash.put(key, FIELD_TENANT, String.valueOf(queueTenant(job.tenantId())));
        hash.put(key, FIELD_STATUS, job.status().name());
        hash.put(key, FIELD_ATTEMPTS, String.valueOf(job.attempts()));
        hash.put(key, FIELD_NEXT, String.valueOf(epochMillis(job.nextAttemptAt())));
        if (job.leaseUntil() == null) {
            hash.delete(key, FIELD_LEASE);
        } else {
            hash.put(key, FIELD_LEASE, String.valueOf(job.leaseUntil().toEpochMilli()));
        }
        if (job.lastError() == null) {
            hash.delete(key, FIELD_ERROR);
        } else {
            hash.put(key, FIELD_ERROR, job.lastError());
        }
        if (streamMessageId != null) {
            hash.put(key, FIELD_STREAM_MSG, streamMessageId);
        }
    }

    private static Instant parseInstant(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString();
        if (text.isBlank() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        return Instant.ofEpochMilli(Long.parseLong(text));
    }

    private static long epochMillis(Instant instant) {
        return instant == null ? Instant.EPOCH.toEpochMilli() : instant.toEpochMilli();
    }

    private String jobKey(long id) {
        return jobKeyPrefix + id;
    }

    private String slotKey(long queueTenant, String type) {
        return slotKeyPrefix + queueTenant + ":" + type;
    }

    private static long queueTenant(Long tenantId) {
        return tenantId == null ? 0L : tenantId;
    }

    private static Long tenantFromQueue(long queueTenant) {
        return queueTenant == 0L ? null : queueTenant;
    }

    private static void validate(String type, Long tenantId) {
        if (type == null || type.isBlank() || tenantId != null && tenantId <= 0) {
            throw new IllegalArgumentException("invalid job");
        }
    }

    private static boolean isBusyGroup(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }
        }
        return false;
    }
}
