package com.gymmind.audit.application;

import com.gymmind.ai.application.AiUsageRecord;

import java.util.List;

public record AiUsagePage(List<AiUsageRecord> content, int page, int size, long totalElements,
                          AiUsageSummary summary) {
}
