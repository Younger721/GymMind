package com.gymmind.ai.application;

import java.math.BigDecimal;
import java.time.Instant;

public record AiUsageRecord(
        Long tenantId,
        Long userId,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        long latencyMs,
        String providerRequestId,
        String status,
        String prompt,
        String provider,
        String feature,
        Integer totalTokens,
        String usageSource,
        BigDecimal estimatedCost,
        String currency,
        Instant createdAt) {

    public AiUsageRecord(Long tenantId, Long userId, String model, int inputTokens, int outputTokens,
                         long latencyMs, String providerRequestId, String status, String prompt) {
        this(tenantId, userId, model, inputTokens, outputTokens, latencyMs, providerRequestId, status, prompt,
                "dashscope", "legacy", inputTokens > 0 || outputTokens > 0 ? inputTokens + outputTokens : null,
                inputTokens > 0 || outputTokens > 0 ? "LEGACY" : "UNAVAILABLE", null, null, null);
    }

    public AiUsageRecord withCreatedAt(Instant value) {
        return new AiUsageRecord(tenantId, userId, model, inputTokens, outputTokens, latencyMs,
                providerRequestId, status, prompt, provider, feature, totalTokens, usageSource,
                estimatedCost, currency, value);
    }
}
