package com.gymmind.knowledge.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** 知识库异步入库任务的重试与租约参数（队列与 Worker 共用）。 */
@Component
public class KnowledgeIngestJobProperties {

    private final int maxAttempts;
    private final long retryBaseSeconds;
    private final Duration leaseDuration;

    public KnowledgeIngestJobProperties(
            @Value("${gymmind.knowledge.ingest.max-attempts:5}") int maxAttempts,
            @Value("${gymmind.knowledge.ingest.retry-base-seconds:30}") long retryBaseSeconds,
            @Value("${gymmind.knowledge.ingest.lease-minutes:5}") long leaseMinutes) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be positive");
        }
        if (retryBaseSeconds < 1) {
            throw new IllegalArgumentException("retryBaseSeconds must be positive");
        }
        if (leaseMinutes < 1) {
            throw new IllegalArgumentException("leaseMinutes must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.retryBaseSeconds = retryBaseSeconds;
        this.leaseDuration = Duration.ofMinutes(leaseMinutes);
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    /** 第 n 次重试（从 1 起）的延迟秒数：base * 2^(n-1)。 */
    public long retryDelaySeconds(int attemptsAfterFailure) {
        if (attemptsAfterFailure < 1) {
            return retryBaseSeconds;
        }
        return retryBaseSeconds << (attemptsAfterFailure - 1);
    }

    public Duration leaseDuration() {
        return leaseDuration;
    }
}
