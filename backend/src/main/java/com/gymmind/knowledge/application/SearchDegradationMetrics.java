package com.gymmind.knowledge.application;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** 单路检索降级次数（Micrometer）。 */
@Component
public class SearchDegradationMetrics {

    private final MeterRegistry registry;

    public SearchDegradationMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordDegraded(String stage) {
        if (stage == null || stage.isBlank() || "none".equals(stage)) {
            return;
        }
        registry.counter("gymmind_search_degraded_total", "stage", stage.trim()).increment();
    }
}
