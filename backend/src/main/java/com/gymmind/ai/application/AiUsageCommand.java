package com.gymmind.ai.application;
public record AiUsageCommand(String model, int inputTokens, int outputTokens, long latencyMs, String providerRequestId, String status, String prompt) {}
