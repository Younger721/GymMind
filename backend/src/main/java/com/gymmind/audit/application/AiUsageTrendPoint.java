package com.gymmind.audit.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AiUsageTrendPoint(LocalDate date, long calls, long inputTokens, long outputTokens,
                                long totalTokens, BigDecimal estimatedCost) {
}
