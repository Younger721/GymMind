package com.gymmind.ai.application;

import java.math.BigDecimal;

public record AiUsageCommand(
        String model,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        long latencyMs,
        String providerRequestId,
        String status,
        String prompt,
        String provider,
        String feature,
        String usageSource,
        BigDecimal estimatedCost,
        String currency) {

    public AiUsageCommand(String model, int inputTokens, int outputTokens, long latencyMs,
                          String providerRequestId, String status, String prompt) {
        this(model, inputTokens, outputTokens,
                inputTokens > 0 || outputTokens > 0 ? inputTokens + outputTokens : null,
                latencyMs, providerRequestId, status, prompt, "dashscope", "legacy",
                inputTokens > 0 || outputTokens > 0 ? "LEGACY" : "UNAVAILABLE", null, null);
    }

    public AiUsageCommand(String model, ModelUsage usage, long latencyMs, String status, String feature) {
        this(model,
                usage == null ? null : usage.inputTokens(),
                usage == null ? null : usage.outputTokens(),
                usage == null ? null : usage.totalTokens(),
                latencyMs,
                usage == null ? null : usage.providerRequestId(),
                status,
                null,
                usage == null ? null : usage.provider(),
                feature,
                usage != null && usage.available() ? "PROVIDER" : "UNAVAILABLE",
                null,
                null);
    }
}
