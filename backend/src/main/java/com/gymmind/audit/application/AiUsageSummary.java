package com.gymmind.audit.application;

import java.math.BigDecimal;

public record AiUsageSummary(long calls, long inputTokens, long outputTokens, long totalTokens,
                             BigDecimal estimatedCost, double averageLatencyMs, double successRate) {
}
