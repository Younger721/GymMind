package com.gymmind.audit.application;

import java.time.Instant;

public record AiUsageFilter(Instant from, Instant to, Long tenantId, String model, String status,
                            int page, int size) {
    public AiUsageFilter {
        page = Math.max(0, page);
        size = Math.min(200, Math.max(1, size));
    }
}
