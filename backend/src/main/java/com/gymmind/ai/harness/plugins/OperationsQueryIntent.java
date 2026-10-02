package com.gymmind.ai.harness.plugins;

import java.util.List;

/** 已完成白名单校验的运营查询意图。 */
public record OperationsQueryIntent(
        List<OperationsMetric> metrics,
        OperationsMetricsQuery query,
        OperationsGroupBy groupBy,
        boolean comparePrevious) {
    public OperationsQueryIntent {
        metrics = metrics == null || metrics.isEmpty()
                ? List.of(OperationsMetric.UNIQUE_VISITORS)
                : List.copyOf(metrics);
        groupBy = groupBy == null ? OperationsGroupBy.NONE : groupBy;
    }
}
