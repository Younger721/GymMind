package com.gymmind.knowledge.application;

import org.springframework.stereotype.Component;

/** 单次 HTTP 检索线程内的降级标记，供响应头读取。 */
@Component
public class KnowledgeSearchDegradationContext {

    public static final String HEADER_NAME = "X-GymMind-Search-Degraded";

    private static final ThreadLocal<String> CURRENT = ThreadLocal.withInitial(() -> "none");

    public void beginSearch() {
        CURRENT.set("none");
    }

    public void applyDegradation(String headerValue) {
        CURRENT.set(headerValue == null || headerValue.isBlank() ? "none" : headerValue.trim());
    }

    public String headerValue() {
        return CURRENT.get();
    }

    public void clear() {
        CURRENT.remove();
    }
}
