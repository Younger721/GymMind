package com.gymmind.ai.domain;

/** 路由结论的来源，便于日志与进度展示。 */
public enum RouteConfidence {
    RULE,
    LLM,
    FALLBACK,
    LEGACY
}
