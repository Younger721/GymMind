package com.gymmind.ai.application;
public record AiUsageRecord(Long tenantId, Long userId, String model, int inputTokens, int outputTokens, long latencyMs, String providerRequestId, String status, String prompt) {}
