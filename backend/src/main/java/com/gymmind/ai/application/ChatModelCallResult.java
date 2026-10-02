package com.gymmind.ai.application;

/** Text response plus upstream usage metadata for one model request. */
public record ChatModelCallResult(String content, ModelUsage usage, long latencyMs, String status) {
    public static ChatModelCallResult unavailable(String content, long latencyMs, String status) {
        return new ChatModelCallResult(content, ModelUsage.unavailable(null, null, null), latencyMs, status);
    }
}
