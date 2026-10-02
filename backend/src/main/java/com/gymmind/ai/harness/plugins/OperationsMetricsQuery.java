package com.gymmind.ai.harness.plugins;

import java.time.LocalDate;

public record OperationsMetricsQuery(LocalDate from, LocalDate to, String periodLabel) {
    public OperationsMetricsQuery {
        if (from == null || to == null || from.isAfter(to)) throw new IllegalArgumentException("Invalid metrics range");
    }
}
